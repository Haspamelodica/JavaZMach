package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

@FunctionalInterface
public interface LocationResolver
{
	public BigInteger locationAbsoluteAddressOrNull(Location location);
}
