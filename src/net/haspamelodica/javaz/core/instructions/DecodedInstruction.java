package net.haspamelodica.javaz.core.instructions;

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
				case LARGE_CONST:
					result.append(String.format("0x%04x", operandVal));
					break;
				case SMALL_CONST:
					result.append(String.format("0x%02x", operandVal));
					break;
				case VARIABLE:
					appendVar(result, operandVal);
					break;
			}
		}
		if(opcode.isStoreOpcode)
		{
			result.append(" -> ");
			appendVar(result, storeTarget);
		}
		if(opcode.isBranchOpcode)
		{
			if(branchOnConditionFalse)
				result.append(" ~");
			else
				result.append(' ');
			if(branchOffset == 0)
				result.append("rfalse");
			else if(branchOffset == 1)
				result.append("rtrue");
			else
				result.append(String.format("0x%04x", branchOffset - 2));
		}
		return result.toString();
	}
	private void appendVar(StringBuilder result, int operandVal)
	{
		if(operandVal == 0)
			result.append("sp[0x00]");
		else if(operandVal < 16)
			result.append(String.format("l%d[0x%02x]", operandVal - 1, operandVal));
		else
			result.append(String.format("g%d[0x%02x]", operandVal - 16, operandVal));
	}
}