package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;

import java.math.BigInteger;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;

public class ResolvableIntegralValue
{
	private final AssemblerIntegralValue integralValue;

	private BigInteger resolvedValue;

	public ResolvableIntegralValue(ResolvedIntegralValue integralValue)
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

	public static ResolvableIntegralValue resolvableIntValOrZero(Optional<ResolvedIntegralValue> value)
	{
		return new ResolvableIntegralValue(value.map(AssemblerIntegralValue::intVal).orElse(intConst(0)));
	}
}
