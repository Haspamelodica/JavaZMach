package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValueOrNull;

import java.math.BigInteger;
import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;

public record AssemblerIntegralValue(Function<LocationResolver, BigInteger> valueFunction)
{
	public BigInteger resolve(LocationResolver locationResolver)
	{
		return valueFunction().apply(locationResolver);
	}

	public static AssemblerIntegralValue intFunc(Function<LocationResolver, BigInteger> valueFunction)
	{
		return new AssemblerIntegralValue(valueFunction);
	}
	public static AssemblerIntegralValue intVal(IntegralValue value)
	{
		return intFunc(r -> integralValueOrNull(value, r));
	}
	public static AssemblerIntegralValue intLoc(Location location)
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
