package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.function.Function;

public interface ValueReferenceEmitter extends SpecialLocationEmitter
{
	public void emitValueReference(ValueReference reference, BigInteger value);
	public void emitValueReferenceHere(ValueReference reference, Function<BigInteger, BigInteger> addrToReferenceValue);

	public default void emitLocationHere(ValueReference reference)
	{
		emitValueReferenceHere(reference, Function.identity());
	}

	// Were this an (abstract) class, then this wouldn't be necessary
	// because the Location variant would override the SpecialLocation variant.
	// Oh well.

	@Override
	public default void emitLocation(SpecialLocation location, BigInteger value)
	{
		emitValueReference((ValueReference) location, value);
	}

	@Override
	public default void emitLocationHere(SpecialLocation location, Function<BigInteger, BigInteger> addrToLocationValue)
	{
		emitValueReferenceHere((ValueReference) location, addrToLocationValue);
	}
}
