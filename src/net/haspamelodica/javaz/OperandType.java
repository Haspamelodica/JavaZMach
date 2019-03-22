package net.haspamelodica.javaz;

public enum OperandType
{
	LARGE_CONST,
	SMALL_CONST,
	VARIABLE;

	public static OperandType decodeTwoBits(int type)
	{
		return type == 0B00 ? LARGE_CONST : type == 0B01 ? SMALL_CONST : type == 0B10 ? VARIABLE : null;
	}
	public static OperandType decodeOneBit(int type)
	{
		return type == 0 ? SMALL_CONST : VARIABLE;
	}
}