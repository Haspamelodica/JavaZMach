package net.haspamelodica.javazmach.assembler.core.valuereferences.manager;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.valuereferences.ValueReference;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.ReferredValue;
import net.haspamelodica.javazmach.assembler.model.values.Variable;

public interface ValueReferenceResolver
{
	/** Returns {@code null} if the location isn't defined, in the first iterations. Other iterations throw an error. */
	public ReferredValue resolveAbsoluteOrNull(ValueReference location);
	/** Returns {@code null} if the location isn't defined, in all iterations. */
	public ReferredValue tryResolveAbsoluteOrNull(ValueReference location);

	/** @see #resolveAbsoluteOrNull(ValueReference) */
	public BigInteger resolveAbsoluteOrNullIntegral(ValueReference location);
	/** @see #tryResolveAbsoluteOrNull(ValueReference) */
	public BigInteger tryResolveAbsoluteOrNullIntegral(ValueReference location);

	/** @see #resolveAbsoluteOrNull(ValueReference) */
	public Variable resolveAbsoluteOrNullVariable(ValueReference location);
	/** @see #tryResolveAbsoluteOrNull(ValueReference) */
	public Variable tryResolveAbsoluteOrNullVariable(ValueReference location);
}
