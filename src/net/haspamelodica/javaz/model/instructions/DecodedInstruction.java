package net.haspamelodica.javaz.model.instructions;

import java.util.Arrays;

import net.haspamelodica.javaz.model.text.ZCharString;

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
	public final ZCharString	text;

	public DecodedInstruction()
	{
		this.operandTypes = new OperandType[8];
		this.operandValues = new int[8];
		this.text = new ZCharString();
	}

	@Override
	public String toString()
	{
		return "DecodedInstruction [opcode=" + opcode + ", form=" + form + ", operandCount=" + operandCount + ", operandTypes=" + Arrays.toString(operandTypes) + ", operandValues=" + Arrays.toString(operandValues) + ", storeTarget=" + storeTarget + ", branchOnConditionTrue=" + branchOnConditionFalse + ", branchOffset=" + branchOffset + ", text=" + text + "]";
	}
}