package net.haspamelodica.javazmach.assembler.model;

public sealed interface Operand permits ConstantInteger, Variable
{
	public boolean isTypeEncodeableUsingOneBit();
	public int encodeTypeOneBit();
	public int encodeTypeTwoBits();
}
