package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralValue;

public class ResolvableCustomDefaultIntegralValue extends ResolvableValue<BigInteger>
{
	public ResolvableCustomDefaultIntegralValue(ResolvedIntegralValue integralValue, BigInteger defaultValue)
	{
		this(intVal(integralValue), defaultValue);
	}
	public ResolvableCustomDefaultIntegralValue(AssemblerIntegralValue integralValue, BigInteger defaultValue)
	{
		super(integralValue::resolve, defaultValue);
	}
}
