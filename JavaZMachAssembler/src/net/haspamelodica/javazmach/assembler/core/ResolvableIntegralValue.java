package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;

import java.math.BigInteger;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public class ResolvableIntegralValue
{
	private final AssemblerIntegralValue integralValue;

	private BigInteger resolvedValue;

	public ResolvableIntegralValue(MacroContext macroContext, IntegralValue integralValue)
	{
		this(intVal(macroContext, integralValue));
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

	public static ResolvableIntegralValue resolvableIntValOrZero(MacroContext macroContext, Optional<IntegralValue> value)
	{
		return new ResolvableIntegralValue(value.map(v -> intVal(macroContext, v)).orElse(intConst(0)));
	}
}
