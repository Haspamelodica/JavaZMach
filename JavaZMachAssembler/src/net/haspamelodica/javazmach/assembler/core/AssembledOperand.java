package net.haspamelodica.javazmach.assembler.core;

public sealed interface AssembledOperand permits AssembledConstantOperand, AssembledVariableOperand
{
	public boolean typeEncodeableOneBit();
	public int encodeTypeOneBitAssumePossible();
	public int encodeTypeTwoBit();
}
