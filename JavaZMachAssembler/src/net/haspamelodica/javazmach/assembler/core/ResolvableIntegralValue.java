package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;

import java.math.BigInteger;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralValue;

public class ResolvableIntegralValue extends ResolvableCustomDefaultIntegralValue
{
	public ResolvableIntegralValue(ResolvedIntegralValue integralValue)
	{
		super(integralValue, ZERO);
	}
	public ResolvableIntegralValue(AssemblerIntegralValue integralValue)
	{
		super(integralValue, ZERO);
	}

	public BigInteger resolvedValueOrZero()
	{
		return resolvedValueOrDefault();
	}

	public static ResolvableIntegralValue resolvableIntValOrZero(Optional<ResolvedIntegralValue> value)
	{
		return new ResolvableIntegralValue(value.map(AssemblerIntegralValue::intVal).orElse(intConst(0)));
	}
}
