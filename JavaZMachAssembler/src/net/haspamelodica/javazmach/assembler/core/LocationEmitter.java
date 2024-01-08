package net.haspamelodica.javazmach.assembler.core;

public interface LocationEmitter extends SpecialLocationEmitter
{
	public void emitLocationHere(Location location);

	// Were this an (abstract) class, then this wouldn't be necessary
	// because the Location variant would override the SpecialLocation variant.
	// Oh well.
	@Override
	public default void emitLocationHere(SpecialLocation location)
	{
		emitLocationHere((Location) location);
	}
}
