package net.haspamelodica.javaz;

import java.util.Arrays;

public class DecodedInstruction
{
	public Opcode				opcode;
	public OpcodeForm			form;
	public int					operandCount;
	public final OperandType[]	operandTypes;
	public final int[]			operandValues;
	public int					storeTarget;
	public boolean				branchOnConditionTrue;
	public int					branchOffset;
	//TODO text

	public DecodedInstruction()
	{
		this.operandTypes = new OperandType[8];
		this.operandValues = new int[8];
	}

	@Override
	public String toString()
	{
		//TODO
		return "DecodedInstruction [opcode=" + opcode + ", form=" + form + ", operandCount=" + operandCount + ", operandTypes=" + Arrays.toString(operandTypes) + ", operandValues=" + Arrays.toString(operandValues) + ", storeTarget=" + storeTarget + ", branchOnConditionTrue=" + branchOnConditionTrue + ", branchOffset=" + branchOffset + "]";
	}
}