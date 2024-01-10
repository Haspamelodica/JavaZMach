package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValueOrNull;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;

public class ResolvableIntegralValue
{
	private final IntegralValue integralValue;

	private BigInteger resolvedValue;

	public ResolvableIntegralValue(IntegralValue integralValue)
	{
		this.integralValue = integralValue;
	}

	public void updateResolvedValue(LocationResolver locationResolver)
	{
		resolvedValue = integralValueOrNull(integralValue, locationResolver);
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
