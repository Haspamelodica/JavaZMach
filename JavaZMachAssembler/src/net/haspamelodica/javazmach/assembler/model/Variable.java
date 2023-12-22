package net.haspamelodica.javazmach.assembler.model;

public sealed interface Variable extends Operand permits StackPointer, LocalVariable, GlobalVariable
{
	@Override
	public default boolean isTypeEncodeableUsingOneBit()
	{
		return true;
	}

	@Override
	public default int encodeTypeOneBit()
	{
		return 1;
	}

	@Override
	public default int encodeTypeTwoBits()
	{
		return 0b10;
	}
}
