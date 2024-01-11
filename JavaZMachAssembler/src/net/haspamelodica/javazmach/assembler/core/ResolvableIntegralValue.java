package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;

import java.math.BigInteger;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;

public class ResolvableIntegralValue
{
	private final AssemblerIntegralValue integralValue;

	private BigInteger resolvedValue;

	public ResolvableIntegralValue(IntegralValue integralValue)
	{
		this(intVal(integralValue));
	}
	public ResolvableIntegralValue(AssemblerIntegralValue integralValue)
	{
		this.integralValue = integralValue;
	}

	public void updateResolvedValue(ValueReferenceResolver valueReferenceResolver)
	{
		resolvedValue = integralValue.resolve(valueReferenceResolver);
	}

	public BigInteger resolvedValueOrNull()
	{
		return resolvedValue;
	}
	public BigInteger resolvedValueOrZero()
	{
		return resolvedValue == null ? ZERO : resolvedValue;
	}

	public static ResolvableIntegralValue resolvableIntValOrZero(Optional<IntegralValue> value)
	{
		return new ResolvableIntegralValue(value.map(AssemblerIntegralValue::intVal).orElse(intConst(0)));
	}
}
