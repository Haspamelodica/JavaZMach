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
	private static final boolean DEBUG_SYSOUTS = false;

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
	private int	globalVariablesOffset;

	private final DecodedInstruction	currentInstr;
	private final int[]					variablesInitialValuesBuf;
	private final int[]					operandEvaluatedValuesBufSigned;
	private final int[]					operandEvaluatedValuesBufUnsigned;

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
		this.operandEvaluatedValuesBufSigned = new int[8];
		this.operandEvaluatedValuesBufUnsigned = new int[8];
	}

	public void reset()
	{
		if(version == 6 || version == 7)
		{
			r_o_8 = 8 * headerParser.getField(RoutinesOffLoc);
			s_o_8 = 8 * headerParser.getField(StringsOffLoc);
		}
		globalVariablesOffset = headerParser.getField(GlobalVarTableLocLoc) - 0x20;
		objectTree.reset();
		if(version == 6)
			doCallTo(headerParser.getField(MainLocLoc), 0, null, 0, true, 0);
		else
		{
			stack.pushCallFrame(-1, 0, variablesInitialValuesBuf, 0, true, 0);
			memAtPC.setAddress(headerParser.getField(InitialPCLoc));//TODO also versions 7-8?
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
			putRawOperandValueToBufs(currentInstr, i);

		boolean doStore = currentInstr.opcode.isStoreOpcode;
		int storeVal = -1;

		boolean branchCondition = false;

		//Opcode ordering and section numbering according to zmach06e.pdf
		//Source: http://mirror.ifarchive.org/indexes/if-archiveXinfocomXinterpretersXspecificationXzspec02.html
		switch(currentInstr.opcode)
		{
			//8.2 Reading and writing memory
			case store:
				writeVariable(operandEvaluatedValuesBufUnsigned[0], operandEvaluatedValuesBufUnsigned[1]);
				break;
			case loadw:
				storeVal = mem.readWord(operandEvaluatedValuesBufUnsigned[0] + (operandEvaluatedValuesBufUnsigned[1] << 1));
				break;
			case storew:
				//TODO enforce header access rules
				dynamicMem.writeWord(operandEvaluatedValuesBufUnsigned[0] + (operandEvaluatedValuesBufUnsigned[1] << 1), operandEvaluatedValuesBufUnsigned[2]);
				break;
			case loadb:
				storeVal = mem.readByte(operandEvaluatedValuesBufUnsigned[0] + operandEvaluatedValuesBufUnsigned[1]);
				break;
			case push:
				stack.push(operandEvaluatedValuesBufUnsigned[0]);
				break;
			case pull_V15:
				writeVariable(operandEvaluatedValuesBufUnsigned[0], stack.pop());
				break;
			//8.3 Arithmetic
			case add:
				storeVal = operandEvaluatedValuesBufUnsigned[0] + operandEvaluatedValuesBufUnsigned[1];
				break;
			case sub:
				storeVal = operandEvaluatedValuesBufUnsigned[0] - operandEvaluatedValuesBufUnsigned[1];
				break;
			case mul:
				storeVal = operandEvaluatedValuesBufUnsigned[0] * operandEvaluatedValuesBufUnsigned[1];
				break;
			case inc:
				int var = operandEvaluatedValuesBufUnsigned[0];
				writeVariable(var, readVariable(var) + 1);
				break;
			case inc_chk://inc_jg in zmach06.pdf
				var = operandEvaluatedValuesBufUnsigned[0];
				int oldVal = readVariable(var);
				writeVariable(var, oldVal + 1);
				//TODO read again or use old value?
				//Makes a difference for variable 0 (Stack)
				branchCondition = readVariable(var) > operandEvaluatedValuesBufSigned[1];
				break;
			case dec_chk://dec_jl in zmach06.pdf
				var = operandEvaluatedValuesBufUnsigned[0];
				oldVal = readVariable(var);
				writeVariable(var, oldVal - 1);
				//TODO read again or use old value?
				//Makes a difference for variable 0 (Stack)
				branchCondition = readVariable(var) < operandEvaluatedValuesBufSigned[1];
				break;
			case and:
				storeVal = operandEvaluatedValuesBufUnsigned[0] & operandEvaluatedValuesBufUnsigned[1];
				break;
			//8.4 Comparison and jumps
			case jz:
				branchCondition = operandEvaluatedValuesBufUnsigned[0] == 0;
				break;
			case je:
				int compareTo = operandEvaluatedValuesBufUnsigned[0];
				branchCondition = false;
				for(int i = 1; i < currentInstr.operandCount; i ++)
					if(compareTo == operandEvaluatedValuesBufUnsigned[i])
					{
						branchCondition = true;
						break;
					}
				break;
			case jl:
				branchCondition = operandEvaluatedValuesBufUnsigned[0] < operandEvaluatedValuesBufUnsigned[1];
				break;
			case jg:
				branchCondition = operandEvaluatedValuesBufUnsigned[0] > operandEvaluatedValuesBufUnsigned[1];
				break;
			case jin:
				branchCondition = objectTree.getParent(operandEvaluatedValuesBufUnsigned[0]) == operandEvaluatedValuesBufUnsigned[1];
				break;
			case test:
				int mask = operandEvaluatedValuesBufUnsigned[1];
				branchCondition = (operandEvaluatedValuesBufUnsigned[0] & mask) == mask;
				break;
			case jump:
				//Sign-extend 16 to 32 bit
				//TODO signed or unsigned?
				memAtPC.skipBytes(((operandEvaluatedValuesBufSigned[0] - 2) << 16) >> 16);
				break;
			//8.5 Call and return, throw and catch
			case call:
				int routinePackedAddr = operandEvaluatedValuesBufUnsigned[0];
				if(routinePackedAddr == 0)
				{
					storeVal = 0;
					break;
				}
				doStore = false;//return will do this store
				doCallTo(routinePackedAddr, currentInstr.operandCount - 1, operandEvaluatedValuesBufUnsigned, 1, false, currentInstr.storeTarget);
				break;
			case ret:
				doReturn(operandEvaluatedValuesBufUnsigned[0]);
				break;
			case rtrue:
				doReturn(1);
				break;
			case rfalse:
				doReturn(0);
				break;
			case ret_popped://ret_pulled in zmach06.pdf
				doReturn(readVariable(0));
				break;
			//8.6 Objects, attributes, and properties
			case get_child:
				storeVal = objectTree.getChild(operandEvaluatedValuesBufUnsigned[0]);
				break;
			case get_parent:
				storeVal = objectTree.getParent(operandEvaluatedValuesBufUnsigned[0]);
				break;
			case insert_obj:
				objectTree.insertObj(operandEvaluatedValuesBufUnsigned[0], operandEvaluatedValuesBufUnsigned[1]);
				break;
			case test_attr:
				branchCondition = objectTree.getAttribute(operandEvaluatedValuesBufUnsigned[0], operandEvaluatedValuesBufUnsigned[1]) == 1;
				break;
			case set_attr:
				objectTree.setAttribute(operandEvaluatedValuesBufUnsigned[0], operandEvaluatedValuesBufUnsigned[1], 1);
				break;
			case put_prop:
				objectTree.putPropOrThrow(operandEvaluatedValuesBufUnsigned[0], operandEvaluatedValuesBufUnsigned[1], operandEvaluatedValuesBufUnsigned[2]);
				break;
			case get_prop:
				storeVal = objectTree.getPropOrDefault(operandEvaluatedValuesBufUnsigned[0], operandEvaluatedValuesBufUnsigned[1]);
				break;
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
				//Sign-extend 14 to 32 bit
				memAtPC.skipBytes(((currentInstr.branchOffset - 2) << 18) >> 18);
		return true;
	}
	private void putRawOperandValueToBufs(DecodedInstruction currentInstr, int i)
	{
		int specifiedVal = currentInstr.operandValues[i];
		switch(currentInstr.operandTypes[i])
		{
			case LARGE_CONST:
				operandEvaluatedValuesBufSigned[i] = specifiedVal;
				operandEvaluatedValuesBufUnsigned[i] = specifiedVal;
				break;
			case SMALL_CONST:
				//Sign-extend 8 to 16 bit
				operandEvaluatedValuesBufSigned[i] = ((specifiedVal << 24) >> 24) & 0xFFFF;
				operandEvaluatedValuesBufUnsigned[i] = specifiedVal;
				break;
			case VARIABLE:
				int val = readVariable(specifiedVal);
				operandEvaluatedValuesBufSigned[i] = val;
				operandEvaluatedValuesBufUnsigned[i] = val;
				break;
			default:
				throw new IllegalArgumentException("Unknown enum type: " + currentInstr.operandTypes[i]);
		}
	}
	private int readVariable(int var)
	{
		if(var == 0)
			return stack.pop();
		else if(var > 0 && var < 0x10)
			return stack.readLocalVariable(var);
		else
			return dynamicMem.readWord(globalVariablesOffset + (var << 1));
	}
	private void writeVariable(int var, int val)
	{
		if(var == 0)
			stack.push(val);
		else if(var > 0 && var < 0x10)
			stack.writeLocalVariable(var, val);
		else
			dynamicMem.writeWord(globalVariablesOffset + (var << 1), val);
	}

	public void doCallTo(int packedRoutineAddress, int suppliedArgumentCount, int[] arguments, int argsOff, boolean discardReturnValue, int storeTarget)
	{
		if(DEBUG_SYSOUTS)
			System.out.println("call");
		int returnPC = memAtPC.getAddress();
		memAtPC.setAddress(packedToByteAddr(packedRoutineAddress, true));
		int specifiedVarCount = memAtPC.readNextByte();
		int variablesCount;
		if(specifiedVarCount >>> 4 == 0)//only the lower 4 bit are allowed to be set
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
			if(readMoreThan15VarsForIllegalVariableCount && specifiedVarCount >>> 4 != 0)
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