package net.haspamelodica.javazmach.assembler.model.values;

public record UnaryExpression(Op op, IntegralValue operand) implements IntegralValue
{
	public static enum Op
	{
		NEGATE,
		BITWISE_NOT;
	}
}
