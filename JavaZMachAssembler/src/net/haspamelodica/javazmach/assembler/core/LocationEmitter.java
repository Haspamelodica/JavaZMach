package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.function.Function;

public interface LocationEmitter extends SpecialLocationEmitter
{
	public void emitLocation(Location location, BigInteger value);
	public void emitLocationHere(Location location, Function<BigInteger, BigInteger> addrToLocationValue);

	public default void emitLocationHere(Location location)
	{
		emitLocationHere(location, Function.identity());
	}

	// Were this an (abstract) class, then this wouldn't be necessary
	// because the Location variant would override the SpecialLocation variant.
	// Oh well.

	@Override
	public default void emitLocation(SpecialLocation location, BigInteger value)
	{
		emitLocation((Location) location, value);
	}

	@Override
	public default void emitLocationHere(SpecialLocation location, Function<BigInteger, BigInteger> addrToLocationValue)
	{
		emitLocationHere((Location) location, addrToLocationValue);
	}
}
