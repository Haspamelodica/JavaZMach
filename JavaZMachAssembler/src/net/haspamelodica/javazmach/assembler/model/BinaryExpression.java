package net.haspamelodica.javazmach.assembler.model;

public record BinaryExpression(IntegralValue lhs, Op op, IntegralValue rhs) implements IntegralValue
{
	public static enum Op
	{
		BITWISE_OR,
		BITWISE_XOR,
		BITWISE_AND,
		LSHIFT,
		RSHIFT,
		ADD,
		SUBTRACT,
		MULTIPLY,
		DIVIDE,
		MODULO;
	}
}
