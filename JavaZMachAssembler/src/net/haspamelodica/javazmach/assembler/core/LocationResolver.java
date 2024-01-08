package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

public interface LocationResolver
{
	public void emitLocationHere(Location location);
	public BigInteger locationAbsoluteAddressOrNull(Location location);
}
