package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class LocationManagerImpl implements LocationManager
{
	private final Map<Location, BigInteger>	backingLocations;
	private final Map<Location, BigInteger>	newlySetLocations;
	private final Supplier<BigInteger>		currentResolvedLocationSupplier;
	private final boolean					permitUndefinedLocations;

	private boolean nextIterationNecessary;

	public LocationManagerImpl(Map<Location, BigInteger> backingLocations, Supplier<BigInteger> currentResolvedLocationSupplier,
			boolean permitUndefinedLocations)
	{
		this.backingLocations = Map.copyOf(backingLocations);
		this.newlySetLocations = new HashMap<>();
		this.currentResolvedLocationSupplier = currentResolvedLocationSupplier;
		this.permitUndefinedLocations = permitUndefinedLocations;

		this.nextIterationNecessary = false;
	}

	@Override
	public BigInteger resolveAbsoluteOrNull(Location location)
	{
		BigInteger newlySetResolved = newlySetLocations.get(location);
		if(newlySetResolved != null)
			return newlySetResolved;

		BigInteger backingResolved = backingLocations.get(location);
		if(backingResolved != null)
			return backingResolved;

		if(permitUndefinedLocations)
		{
			nextIterationNecessary = true;
			return null;
		}

		// No need to go through a custom DiagnosticHandler:
		// Only in the first iteration is this possible, and there permitUndefinedLocations is set.
		return DiagnosticHandler.defaultError("Undefined location: " + location);
	}

	@Override
	public void emitLocationHere(Location location)
	{
		BigInteger newResolved = currentResolvedLocationSupplier.get();

		BigInteger oldNewlySetResolved = newlySetLocations.put(location, newResolved);
		if(oldNewlySetResolved != null)
			DiagnosticHandler.defaultError("Location defined twice in one iteration: " + location);

		// This also handles the case where the given location didn't exist in backingLocations:
		// get will return null, and thus equals will return false.
		nextIterationNecessary |= !newResolved.equals(backingLocations.get(location));
	}

	public boolean nextIterationNecessary()
	{
		return nextIterationNecessary;
	}

	public Map<Location, BigInteger> getNewlySetLocations()
	{
		return newlySetLocations;
	}
}
