package net.haspamelodica.javazmach.assembler.model;

public sealed interface Operand permits ConstantInteger, Variable
{
	boolean isTypeEncodeableUsingOneBit();
}
