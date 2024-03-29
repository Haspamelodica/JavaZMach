package net.haspamelodica.javazmach.assembler.core.valuereferences.manager;

import java.math.BigInteger;
import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.core.valuereferences.SpecialLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.IntegralReferredValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.ReferredValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.VariableReferredValue;
import net.haspamelodica.javazmach.assembler.model.values.Variable;

public interface SpecialLocationEmitter
{
	public void emitLocation(SpecialLocation location, ReferredValue value);
	public void emitLocationHere(SpecialLocation location, Function<BigInteger, BigInteger> addrToLocationValue);

	public default void emitLocation(SpecialLocation location, BigInteger value)
	{
		emitLocation(location, new IntegralReferredValue(value));
	}
	public default void emitLocation(SpecialLocation location, Variable value)
	{
		emitLocation(location, new VariableReferredValue(value));
	}

	public default void emitLocationHere(SpecialLocation location)
	{
		emitLocationHere(location, Function.identity());
	}
}
