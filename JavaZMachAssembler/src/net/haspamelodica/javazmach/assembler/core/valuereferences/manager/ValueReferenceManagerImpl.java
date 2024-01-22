package net.haspamelodica.javazmach.assembler.core.valuereferences.manager;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.haspamelodica.javazmach.assembler.core.valuereferences.ValueReference;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.ReferredValue;

public class ValueReferenceManagerImpl extends SimpleValueReferenceResolver implements ValueReferenceManager
{
	private final Map<ValueReference, ReferredValue>	backingValueReference;
	private final Map<ValueReference, ReferredValue>	newlySetValueReferences;
	private final Supplier<BigInteger>					currentAddrSupplier;
	private final boolean								permitUndefinedValueReferences;

	private boolean nextIterationNecessary;

	public ValueReferenceManagerImpl(Map<ValueReference, ReferredValue> backingLocations, Supplier<BigInteger> currentAddrSupplier, boolean permitUndefinedLocations)
	{
		this.backingValueReference = Map.copyOf(backingLocations);
		this.newlySetValueReferences = new HashMap<>();
		this.currentAddrSupplier = currentAddrSupplier;
		this.permitUndefinedValueReferences = permitUndefinedLocations;

		this.nextIterationNecessary = false;
	}

	@Override
	public ReferredValue resolveAbsoluteOrNull(ValueReference valueRef)
	{
		ReferredValue result = tryResolveAbsoluteOrNull(valueRef);
		if(result != null)
			return result;

		return emitErrorOrNull("Undefined location: " + valueRef);
	}

	@Override
	public <R> R emitErrorOrNull(String message)
	{
		if(permitUndefinedValueReferences)
		{
			nextIterationNecessary = true;
			return null;
		}

		// No need to go through a custom DiagnosticHandler: this method should throw an error in all except the first iteration,
		// and there permitUndefinedValueReferences is set.
		return defaultError(message);
	}

	@Override
	public ReferredValue tryResolveAbsoluteOrNull(ValueReference valueRef)
	{
		ReferredValue newlySetResolved = newlySetValueReferences.get(valueRef);
		if(newlySetResolved != null)
			return newlySetResolved;

		ReferredValue backingResolved = backingValueReference.get(valueRef);
		if(backingResolved != null)
			return backingResolved;

		return null;
	}

	@Override
	public void emitValueReference(ValueReference valueRef, ReferredValue value)
	{
		ReferredValue oldNewlySetResolved = newlySetValueReferences.put(valueRef, value);
		if(oldNewlySetResolved != null)
			defaultError("Location defined twice in one iteration: " + valueRef);

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

	public Map<ValueReference, ReferredValue> getNewlySetLocationsModifiable()
	{
		return newlySetValueReferences;
	}
}
