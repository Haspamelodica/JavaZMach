package net.haspamelodica.javazmach.core.instructions;

import java.math.BigInteger;

public class DecodedInstruction
{
	public Opcode				opcode;
	public OpcodeForm			form;
	public int					operandCount;
	public final OperandType[]	operandTypes;
	public final int[]			operandValues;
	public int					storeTarget;
	public boolean				branchOnConditionFalse;
	public int					branchOffset;

	public DecodedInstruction()
	{
		this.operandTypes = new OperandType[8];
		this.operandValues = new int[8];
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		result.append(opcode.name);
		result.append('.');
		result.append(form.shortName);
		for(int i = 0; i < operandCount; i ++)
		{
			result.append(' ');
			int operandVal = operandValues[i];
			switch(operandTypes[i])
			{
				case LARGE_CONST -> result.append((operandVal & 0x8000) == 0
						? String.format("%0#4x", operandVal)
						// using BigInteger to force %x to not emit 0xfff... for negative values
						// cast to short to do sign-extension
						: String.format("%0#4x[%0#4x]", operandVal, BigInteger.valueOf((short) operandVal)));
				case SMALL_CONST -> result.append(String.format("%0#2x", operandVal));
				case VARIABLE -> appendVar(result, operandVal);
			}
		}
		if(opcode.isStoreOpcode)
		{
			result.append(" -> ");
			appendVar(result, storeTarget);
		}
		if(opcode.isBranchOpcode)
		{
			result.append(" ?");
			if(branchOnConditionFalse)
				result.append('~');
			if(branchOffset == 0)
				result.append("rfalse");
			else if(branchOffset == 1)
				result.append("rtrue");
			else
				// using BigInteger to force %x to not emit 0xfff... for negative values
				result.append(String.format("%0#4x", BigInteger.valueOf(branchOffset - 2)));
		}
		return result.toString();
	}
	private void appendVar(StringBuilder result, int operandVal)
	{
		if(operandVal == 0)
			result.append("sp[0x00]");
		else if(operandVal < 16)
			result.append(String.format("l%d[%0#2x]", operandVal - 1, operandVal));
		else
			result.append(String.format("g%d[%0#2x]", operandVal - 16, operandVal));
	}
}
