package net.haspamelodica.javaz.model;

import static net.haspamelodica.javaz.model.HeaderParser.GlobalVarTableLocLoc;
import static net.haspamelodica.javaz.model.HeaderParser.InitialPCLoc;
import static net.haspamelodica.javaz.model.HeaderParser.MainLocLoc;
import static net.haspamelodica.javaz.model.HeaderParser.RoutinesOffLoc;
import static net.haspamelodica.javaz.model.HeaderParser.StringsOffLoc;
import static net.haspamelodica.javaz.model.HeaderParser.VersionLoc;

import java.util.Arrays;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.model.instructions.DecodedInstruction;
import net.haspamelodica.javaz.model.instructions.InstructionDecoder;
import net.haspamelodica.javaz.model.instructions.OperandType;
import net.haspamelodica.javaz.model.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.model.memory.SequentialMemoryAccess;
import net.haspamelodica.javaz.model.memory.WritableMemory;
import net.haspamelodica.javaz.model.objects.ObjectTree;
import net.haspamelodica.javaz.model.stack.CallStack;

public class ZInterpreter
{
	private static final boolean DEBUG_SYSOUTS = true;

	private final int version;

	private final boolean	dontIgnoreIllegalVariableCount;
	private final boolean	readMoreThan15VarsForIllegalVariableCount;

	private final HeaderParser				headerParser;
	private final WritableMemory			dynamicMem;
	private final ReadOnlyMemory			mem;
	private final CallStack					stack;
	private final SequentialMemoryAccess	memAtPC;
	private final InstructionDecoder		instrDecoder;
	private final ObjectTree				objectTree;

	private int	r_o_8;
	private int	s_o_8;
	private int	globalVariablesTableLoc;

	private final DecodedInstruction	currentInstr;
	private final int[]					variablesInitialValuesBuf;
	private final int[]					operandEvaluatedValuesBuf;

	public ZInterpreter(GlobalConfig config, WritableMemory dynamicMem, ReadOnlyMemory mem)
	{
		this(config, -1, dynamicMem, mem);
	}
	public ZInterpreter(GlobalConfig config, int versionOverride, WritableMemory dynamicMem, ReadOnlyMemory mem)
	{
		this.headerParser = new HeaderParser(dynamicMem);
		this.version = versionOverride > 0 ? versionOverride : headerParser.getField(VersionLoc);

		this.dontIgnoreIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.dont_ignore");
		this.readMoreThan15VarsForIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.allow_read_more_than_15_vars");

		this.dynamicMem = dynamicMem;
		this.mem = mem;
		this.stack = new CallStack();
		this.memAtPC = new SequentialMemoryAccess(mem);
		this.instrDecoder = new InstructionDecoder(config, version, memAtPC);
		this.objectTree = new ObjectTree(config, version, headerParser, dynamicMem);

		this.currentInstr = new DecodedInstruction();
		this.variablesInitialValuesBuf = new int[16];
		this.operandEvaluatedValuesBuf = new int[8];
	}

	public void reset()
	{
		if(version == 6 || version == 7)
		{
			r_o_8 = 8 * headerParser.getField(RoutinesOffLoc);
			s_o_8 = 8 * headerParser.getField(StringsOffLoc);
		}
		globalVariablesTableLoc = headerParser.getField(GlobalVarTableLocLoc);
		objectTree.reset();
		if(version == 6)
			doCallTo(headerParser.getField(MainLocLoc), 0, null, 0, true, 0);
		else
		{
			stack.pushCallFrame(-1, 0, variablesInitialValuesBuf, 0, true, 0);
			memAtPC.setAddress(headerParser.getField(InitialPCLoc));
		}
		if(DEBUG_SYSOUTS)
			System.out.println("Reset complete!");
	}
	/**
	 * Returns true if the game should continue (=is not finished)
	 */
	public boolean step()
	{
		int currentInstrPC = memAtPC.getAddress();
		instrDecoder.decode(currentInstr);
		if(DEBUG_SYSOUTS)
		{
			System.out.printf("pc=%05x (to %05x): ", currentInstrPC, memAtPC.getAddress() - 1);
			System.out.println(currentInstr);
		}
		for(int i = 0; i < currentInstr.operandCount; i ++)
			operandEvaluatedValuesBuf[i] = getRawOperandValue(currentInstr.operandTypes[i], currentInstr.operandValues[i]);

		boolean doStore = currentInstr.opcode.isStoreOpcode;
		int storeVal = -1;

		boolean branchCondition = false;

		//Opcode ordering and section numbering according to zmach06e.pdf
		//Source: http://mirror.ifarchive.org/indexes/if-archiveXinfocomXinterpretersXspecificationXzspec02.html
		switch(currentInstr.opcode)
		{
			//8.2 Reading and writing memory
			case loadw:
				storeVal = mem.readWord(operandEvaluatedValuesBuf[0] + (operandEvaluatedValuesBuf[1] << 1));
				break;
			case storew:
				dynamicMem.writeWord(operandEvaluatedValuesBuf[0] + (operandEvaluatedValuesBuf[1] << 1), operandEvaluatedValuesBuf[2]);
				break;
			//8.3 Arithmetic
			case add:
				storeVal = operandEvaluatedValuesBuf[0] + operandEvaluatedValuesBuf[1];
				break;
			case sub:
				storeVal = operandEvaluatedValuesBuf[0] - operandEvaluatedValuesBuf[1];
				break;
			//8.4 Comparison and jumps
			case jz:
				branchCondition = operandEvaluatedValuesBuf[0] == 0;
				break;
			case je:
				int compareTo = operandEvaluatedValuesBuf[0];
				branchCondition = false;
				for(int i = 1; i < currentInstr.operandCount; i ++)
					if(compareTo == operandEvaluatedValuesBuf[i])
					{
						branchCondition = true;
						break;
					}
				break;
			case jump:
				//TODO Is the branch offset signed if jump is assembled with operand type SMALL_CONSTANT?
				//I assume it is not (because it's easier to implement ;))
				//Sign-extend word to Java integer. For an explanation see below.
				memAtPC.skipBytes(((operandEvaluatedValuesBuf[0] - 2) << 16) >> 16);
				break;
			//8.5 Call and return, throw and catch
			case call:
				int routinePackedAddr = operandEvaluatedValuesBuf[0];
				if(routinePackedAddr == 0)
				{
					storeVal = 0;
					break;
				}
				doStore = false;//return will do this store
				doCallTo(routinePackedAddr, currentInstr.operandCount - 1, operandEvaluatedValuesBuf, 1, false, currentInstr.storeTarget);
				break;
			case ret:
				doReturn(operandEvaluatedValuesBuf[0]);
				break;
			//8.6 Objects, attributes, and properties
			//8.7 Windows
			//8.8 Input and output streams
			//8.9 Input
			//8.10 Character based output
			//8.11 Miscellaneous screen output
			//8.12 Sound, mouse, and menus
			//8.13 Save, restore, and undo
			//8.14 Miscellaneous
			default:
				throw new IllegalStateException("Instruction not yet implemented: " + currentInstr.opcode);
		}
		if(doStore)
			writeVariable(currentInstr.storeTarget, storeVal);
		if(currentInstr.opcode.isBranchOpcode && (branchCondition ^ currentInstr.branchOnConditionFalse))
			if(currentInstr.branchOffset == 0)
				doReturn(0);
			else if(currentInstr.branchOffset == 1)
				doReturn(1);
			else
				//sign-extend the branch offset (ab)using shifts:
				//0000 0000  0000 0000  0011 1111  1111 1111   highest branch offset (14 bits, signed)
				//1111 1111  1111 1111  1111 1111  1111 1111   max int (32 bits, signed)
				//<---18 (32-14) bits---->
				memAtPC.skipBytes(((currentInstr.branchOffset - 2) << 18) >> 18);
		return true;
	}
	private int getRawOperandValue(OperandType type, int val)
	{
		switch(type)
		{
			case LARGE_CONST:
			case SMALL_CONST:
				return val;
			case VARIABLE:
				return readVariable(val);
			default:
				throw new IllegalArgumentException("Unknown enum type: " + type);
		}
	}
	private int readVariable(int var)
	{
		if(var == 0)
			return stack.pop();
		else if(var > 0 && var < 0x10)
			return stack.readLocalVariable(var);
		else
			return dynamicMem.readWord(globalVariablesTableLoc + var - 0x10);
	}
	private void writeVariable(int var, int val)
	{
		if(var == 0)
			stack.push(val);
		else if(var > 0 && var < 0x10)
			stack.writeLocalVariable(var, val);
		else
			dynamicMem.writeWord(globalVariablesTableLoc + var - 0x10, val);
	}

	public void doCallTo(int packedRoutineAddress, int suppliedArgumentCount, int[] arguments, int argsOff, boolean discardReturnValue, int storeTarget)
	{
		if(DEBUG_SYSOUTS)
			System.out.println("call");
		int returnPC = memAtPC.getAddress();
		memAtPC.setAddress(packedToByteAddr(packedRoutineAddress, true));
		int specifiedVarCount = memAtPC.readNextByte();
		int variablesCount;
		if(specifiedVarCount >> 4 == 0)//only the lower 4 bit are allowed to be set
			variablesCount = specifiedVarCount;
		else if(dontIgnoreIllegalVariableCount)
			throw new VariableException("Illegal variable count: " + specifiedVarCount);
		else
			variablesCount = 15;//the maximum we can supply
		suppliedArgumentCount = Math.min(suppliedArgumentCount, variablesCount);//discard last arguments if there are too many
		if(version < 5)
		{
			memAtPC.skipWords(suppliedArgumentCount);//skip overwritten initial values
			for(int i = suppliedArgumentCount; i < variablesCount; i ++)
				variablesInitialValuesBuf[i] = memAtPC.readNextWord();
			if(readMoreThan15VarsForIllegalVariableCount && specifiedVarCount >> 4 != 0)
				memAtPC.skipWords(specifiedVarCount - 15);
		} else
			Arrays.fill(variablesInitialValuesBuf, suppliedArgumentCount, variablesCount, 0);
		for(int i = 0; i < suppliedArgumentCount; i ++)
			variablesInitialValuesBuf[i] = arguments[i + argsOff];
		stack.pushCallFrame(returnPC, variablesCount, variablesInitialValuesBuf, suppliedArgumentCount, discardReturnValue, storeTarget);
	}
	public void doReturn(int returnVal)
	{
		if(DEBUG_SYSOUTS)
			System.out.println("return");
		boolean discardReturnValue = stack.getCurrentCallFrameDiscardReturnValue();
		int storeTarget = stack.getCurrentCallFrameStoreTarget();
		memAtPC.setAddress(stack.popCallFrame());
		if(stack.getCurrentCallFrameFP() <= 0)
			throw new ControlFlowException("Return from main routine");
		if(!discardReturnValue)
			//TODO interrupt routines
			writeVariable(storeTarget, returnVal);
	}
	public int packedToByteAddr(int packed, boolean isRoutine)
	{
		switch(version)
		{
			case 1:
			case 2:
			case 3:
				return packed << 1;
			case 4:
			case 5:
				return packed << 2;
			case 6:
			case 7:
				return (packed << 2) + (isRoutine ? r_o_8 : s_o_8);
			case 8:
				return packed << 3;
			default:
				throw new VersionException("Unknown version: " + version);
		}
	}
}