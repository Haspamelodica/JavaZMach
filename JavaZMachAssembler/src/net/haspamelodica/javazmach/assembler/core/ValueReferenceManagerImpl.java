package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ValueReferenceManagerImpl implements ValueReferenceManager
{
	private final Map<ValueReference, BigInteger>	backingValueReference;
	private final Map<ValueReference, BigInteger>	newlySetValueReferences;
	private final Supplier<BigInteger>				currentAddrSupplier;
	private final boolean							permitUndefinedValueReferences;

	private boolean nextIterationNecessary;

	public ValueReferenceManagerImpl(Map<ValueReference, BigInteger> backingLocations, Supplier<BigInteger> currentAddrSupplier, boolean permitUndefinedLocations)
	{
		this.backingValueReference = Map.copyOf(backingLocations);
		this.newlySetValueReferences = new HashMap<>();
		this.currentAddrSupplier = currentAddrSupplier;
		this.permitUndefinedValueReferences = permitUndefinedLocations;

		this.nextIterationNecessary = false;
	}

	@Override
	public BigInteger resolveAbsoluteOrNull(ValueReference valueRef)
	{
		BigInteger result = tryResolveAbsoluteOrNull(valueRef);
		if(result != null)
			return result;

		if(permitUndefinedValueReferences)
		{
			nextIterationNecessary = true;
			return null;
		}

		// No need to go through a custom DiagnosticHandler:
		// Only in the first iteration is this possible, and there permitUndefinedLocations is set.
		return DiagnosticHandler.defaultError("Undefined location: " + valueRef);
	}

	@Override
	public BigInteger tryResolveAbsoluteOrNull(ValueReference valueRef)
	{
		BigInteger newlySetResolved = newlySetValueReferences.get(valueRef);
		if(newlySetResolved != null)
			return newlySetResolved;

		BigInteger backingResolved = backingValueReference.get(valueRef);
		if(backingResolved != null)
			return backingResolved;

		return null;
	}

	@Override
	public void emitValueReference(ValueReference valueRef, BigInteger value)
	{
		BigInteger oldNewlySetResolved = newlySetValueReferences.put(valueRef, value);
		if(oldNewlySetResolved != null)
			DiagnosticHandler.defaultError("Location defined twice in one iteration: " + valueRef);

		// This also handles the case where the given location didn't exist in backingLocations:
		// get will return null, and thus equals will return false.
		nextIterationNecessary |= !value.equals(backingValueReference.get(valueRef));
	}

	@Override
	public void emitValueReferenceHere(ValueReference valueRef, Function<BigInteger, BigInteger> addrToLocationValue)
	{
		emitValueReference(valueRef, addrToLocationValue.apply(currentAddrSupplier.get()));
	}

	public boolean nextIterationNecessary()
	{
		return nextIterationNecessary;
	}

	public Map<ValueReference, BigInteger> getNewlySetLocationsModifiable()
	{
		return newlySetValueReferences;
	}
}
