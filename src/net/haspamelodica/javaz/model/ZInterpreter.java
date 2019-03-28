package net.haspamelodica.javaz.model;

import java.util.Arrays;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.model.instructions.DecodedInstruction;
import net.haspamelodica.javaz.model.instructions.InstructionDecoder;
import net.haspamelodica.javaz.model.instructions.OperandType;
import net.haspamelodica.javaz.model.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.model.memory.SequentialMemoryAccess;
import net.haspamelodica.javaz.model.memory.WritableMemory;
import net.haspamelodica.javaz.model.stack.CallStack;

public class ZInterpreter
{
	private final int version;

	private final boolean	dontIgnoreIllegalVariableCount;
	private final boolean	readMoreThan15VarsForIllegalVariableCount;

	private final WritableMemory			dynamicMem;
	private final ReadOnlyMemory			mem;
	private final CallStack					stack;
	private final HeaderParser				headerParser;
	private final SequentialMemoryAccess	memAtPC;
	private final InstructionDecoder		instrDecoder;

	private int	r_o_8;
	private int	s_o_8;
	private int	globalVariablesTableLoc;

	private final DecodedInstruction	currentInstr;
	private final int[]					variablesInitialValuesBuf;
	private final int[]					operandEvaluatedValuesBuf;

	public ZInterpreter(GlobalConfig config, int versionOverride, WritableMemory dynamicMem, ReadOnlyMemory mem)
	{
		this.headerParser = new HeaderParser(dynamicMem);
		this.version = versionOverride > 0 ? versionOverride : headerParser.getField(HeaderParser.VersionLoc);

		this.dontIgnoreIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.dont_ignore");
		this.readMoreThan15VarsForIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.allow_read_more_than_15_vars");

		this.dynamicMem = dynamicMem;
		this.mem = mem;
		this.stack = new CallStack();
		this.memAtPC = new SequentialMemoryAccess(mem);
		this.instrDecoder = new InstructionDecoder(config, version, memAtPC);
		this.currentInstr = new DecodedInstruction();
		this.variablesInitialValuesBuf = new int[16];
		this.operandEvaluatedValuesBuf = new int[8];
	}

	public void reset()
	{
		if(version == 6 || version == 7)
		{
			r_o_8 = 8 * headerParser.getField(HeaderParser.RoutinesOffLoc);
			s_o_8 = 8 * headerParser.getField(HeaderParser.StringsOffLoc);
		}
		globalVariablesTableLoc = headerParser.getField(HeaderParser.GlobalVarTableLocLoc);
		if(version == 6)
			doCallTo(headerParser.getField(HeaderParser.MainLocLoc), 0, null, 0, true, 0);
		else
		{
			stack.pushCallFrame(-1, 0, variablesInitialValuesBuf, 0, true, 0);
			memAtPC.setAddress(headerParser.getField(HeaderParser.InitialPCLoc));
		}
	}
	/**
	 * Returns true if the game should continue (=is not finished)
	 */
	public boolean step()
	{
		instrDecoder.decode(currentInstr);
		System.out.printf("pc=%05x: ", memAtPC.getAddress());
		System.out.println(currentInstr);
		for(int i = 0; i < currentInstr.operandCount; i ++)
			operandEvaluatedValuesBuf[i] = getRawOperandValue(currentInstr.operandTypes[i], currentInstr.operandValues[i]);
		boolean doStore = currentInstr.opcode.isStoreOpcode;
		int storeVal = -1;
		switch(currentInstr.opcode)
		{
			case add:
				storeVal = operandEvaluatedValuesBuf[0] + operandEvaluatedValuesBuf[1];
				break;
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
			default:
				throw new IllegalStateException("Instruction not yet implemented: " + currentInstr.opcode);
		}
		if(doStore)
			writeVariable(currentInstr.storeTarget, storeVal);
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