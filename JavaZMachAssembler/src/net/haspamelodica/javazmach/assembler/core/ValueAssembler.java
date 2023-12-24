package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.BinaryExpression;
import net.haspamelodica.javazmach.assembler.model.CharLiteral;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.LabelReference;
import net.haspamelodica.javazmach.assembler.model.NumberLiteral;
import net.haspamelodica.javazmach.assembler.model.UnaryExpression;

public class ValueAssembler
{
	private final LabelResolver labelResolver;

	public ValueAssembler(LabelResolver labelResolver)
	{
		this.labelResolver = labelResolver;
	}

	public BigInteger integralValue(IntegralValue value)
	{
		return switch(value)
		{
			case NumberLiteral literal -> literal.value();
			case CharLiteral literal -> BigInteger.valueOf(literal.value());
			case LabelReference labelRef -> BigInteger.valueOf(labelResolver.labelValue(labelRef.name()));
			case BinaryExpression expr ->
			{
				BigInteger lhs = integralValue(expr.lhs());
				BigInteger rhs = integralValue(expr.rhs());
				yield switch(expr.op())
				{
					case BITWISE_OR -> lhs.or(rhs);
					case BITWISE_XOR -> lhs.xor(rhs);
					case BITWISE_AND -> lhs.and(rhs);
					case LSHIFT -> lhs.shiftLeft(rhs.intValueExact());
					case RSHIFT -> lhs.shiftRight(rhs.intValueExact());
					case ADD -> lhs.add(rhs);
					case SUBTRACT -> lhs.subtract(rhs);
					case MULTIPLY -> lhs.multiply(rhs);
					case DIVIDE -> lhs.divide(rhs);
					case MODULO -> lhs.mod(rhs);
				};
			}
			case UnaryExpression expr ->
			{
				BigInteger operand = integralValue(expr.operand());
				yield switch(expr.op())
				{
					case NEGATE -> operand.negate();
					case BITWISE_NOT -> operand.not();
				};
			}
		};
	}
}
