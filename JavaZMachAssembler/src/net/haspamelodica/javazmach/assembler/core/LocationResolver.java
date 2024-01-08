package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

public interface LocationResolver
{
	public BigInteger resolveAbsoluteOrNull(Location location);
}
