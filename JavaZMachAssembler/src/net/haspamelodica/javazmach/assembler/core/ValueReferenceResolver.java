package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

public interface ValueReferenceResolver
{
	public BigInteger resolveAbsoluteOrNull(ValueReference location);
}
