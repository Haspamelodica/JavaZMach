package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

public class ResolvableIntegralValue
{
	private final AssemblerIntegralValue integralValue;

	private BigInteger resolvedValue;

	public ResolvableIntegralValue(AssemblerIntegralValue integralValue)
	{
		this.integralValue = integralValue;
	}

	public void updateResolvedValue(LocationResolver locationResolver)
	{
		resolvedValue = integralValue.resolve(locationResolver);
	}

	public BigInteger resolvedValueOrNull()
	{
		return resolvedValue;
	}
	public BigInteger resolvedValueOrZero()
	{
		return resolvedValue == null ? ZERO : resolvedValue;
	}
}
