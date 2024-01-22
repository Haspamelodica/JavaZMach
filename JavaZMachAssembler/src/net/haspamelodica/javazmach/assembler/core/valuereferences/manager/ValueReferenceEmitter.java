package net.haspamelodica.javazmach.assembler.core.valuereferences.manager;

import java.math.BigInteger;
import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.core.valuereferences.SpecialLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.ValueReference;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.IntegralReferredValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.ReferredValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.VariableReferredValue;
import net.haspamelodica.javazmach.assembler.model.values.Variable;

public interface ValueReferenceEmitter extends SpecialLocationEmitter
{
	public void emitValueReference(ValueReference reference, ReferredValue value);
	public void emitValueReferenceHere(ValueReference reference, Function<BigInteger, BigInteger> addrToReferenceValue);

	public default void emitValueReference(ValueReference reference, BigInteger value)
	{
		emitValueReference(reference, new IntegralReferredValue(value));
	}
	public default void emitValueReference(ValueReference reference, Variable value)
	{
		emitValueReference(reference, new VariableReferredValue(value));
	}

	public default void emitLocationHere(ValueReference reference)
	{
		emitValueReferenceHere(reference, Function.identity());
	}

	// Were this an (abstract) class, then this wouldn't be necessary
	// because the Location variant would override the SpecialLocation variant.
	// Oh well.

	@Override
	public default void emitLocation(SpecialLocation location, ReferredValue value)
	{
		emitValueReference((ValueReference) location, value);
	}

	@Override
	public default void emitLocationHere(SpecialLocation location, Function<BigInteger, BigInteger> addrToLocationValue)
	{
		emitValueReferenceHere((ValueReference) location, addrToLocationValue);
	}
}
