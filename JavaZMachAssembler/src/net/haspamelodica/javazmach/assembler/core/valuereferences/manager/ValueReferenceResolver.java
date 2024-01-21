package net.haspamelodica.javazmach.assembler.core.valuereferences.manager;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.valuereferences.ValueReference;

public interface ValueReferenceResolver
{
	/** Returns {@code null} if the location isn't defined, in the first iterations. Other iterations throw an error. */
	public BigInteger resolveAbsoluteOrNull(ValueReference location);
	/** Returns {@code null} if the location isn't defined, in all iterations. */
	public BigInteger tryResolveAbsoluteOrNull(ValueReference location);
}
