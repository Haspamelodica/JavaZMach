package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValueOrNull;

import java.math.BigInteger;
import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.ValueReference;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;

public record AssemblerIntegralValue(Function<ValueReferenceResolver, BigInteger> valueFunction)
{
	public BigInteger resolve(ValueReferenceResolver valueReferenceResolver)
	{
		return valueFunction().apply(valueReferenceResolver);
	}

	public static AssemblerIntegralValue intFunc(Function<ValueReferenceResolver, BigInteger> valueFunction)
	{
		return new AssemblerIntegralValue(valueFunction);
	}
	public static AssemblerIntegralValue intVal(ResolvedIntegralValue value)
	{
		return intFunc(r -> integralValueOrNull(value, r));
	}
	public static AssemblerIntegralValue intLoc(ValueReference location)
	{
		return intFunc(r -> r.resolveAbsoluteOrNull(location));
	}
	public static AssemblerIntegralValue intConst(BigInteger value)
	{
		return intFunc(r -> value);
	}
	public static AssemblerIntegralValue intConst(int value)
	{
		return intConst(BigInteger.valueOf(value));
	}
}
