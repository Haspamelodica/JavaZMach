package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.function.Function;

public interface SpecialLocationEmitter
{
	public void emitLocationHere(Function<BigInteger, BigInteger> addrToLocationValue, SpecialLocation location);

	public default void emitLocationHere(SpecialLocation location)
	{
		emitLocationHere(Function.identity(), location);
	}
}
