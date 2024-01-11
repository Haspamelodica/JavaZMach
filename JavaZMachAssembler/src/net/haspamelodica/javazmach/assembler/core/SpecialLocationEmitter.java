package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.function.Function;

public interface SpecialLocationEmitter
{
	public void emitLocation(SpecialLocation location, BigInteger value);
	public void emitLocationHere(SpecialLocation location, Function<BigInteger, BigInteger> addrToLocationValue);

	public default void emitLocationHere(SpecialLocation location)
	{
		emitLocationHere(location, Function.identity());
	}
}
