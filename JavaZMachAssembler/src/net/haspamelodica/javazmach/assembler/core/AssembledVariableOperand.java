package net.haspamelodica.javazmach.assembler.core;

public record AssembledVariableOperand() implements AssembledOperand
{
	@Override
	public boolean typeEncodeableOneBit()
	{
		return true;
	}

	@Override
	public int encodeTypeOneBitAssumePossible()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int encodeTypeTwoBit()
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
