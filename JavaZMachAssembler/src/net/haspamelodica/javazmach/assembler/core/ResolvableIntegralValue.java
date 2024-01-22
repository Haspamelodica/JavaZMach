package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;

import java.math.BigInteger;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralValue;

public class ResolvableIntegralValue extends ResolvableValue<BigInteger>
{
	public ResolvableIntegralValue(ResolvedIntegralValue integralValue)
	{
		this(intVal(integralValue));
	}
	public ResolvableIntegralValue(AssemblerIntegralValue integralValue)
	{
		super(integralValue::resolve, ZERO);
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
