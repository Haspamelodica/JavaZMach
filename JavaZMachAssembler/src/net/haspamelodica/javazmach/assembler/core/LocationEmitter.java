package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.function.Function;

public interface LocationEmitter extends SpecialLocationEmitter
{
	public void emitLocationHere(Function<BigInteger, BigInteger> addrToLocationValue, Location location);

	public default void emitLocationHere(Location location)
	{
		emitLocationHere(Function.identity(), location);
	}

	// Were this an (abstract) class, then this wouldn't be necessary
	// because the Location variant would override the SpecialLocation variant.
	// Oh well.
	@Override
	public default void emitLocationHere(Function<BigInteger, BigInteger> addrToLocationValue, SpecialLocation location)
	{
		emitLocationHere(addrToLocationValue, (Location) location);
	}
}
