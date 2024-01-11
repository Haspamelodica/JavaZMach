package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.assembler.model.SectionDeclaration;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledSectionDeclaration implements AssembledEntry
{
	private final SpecialLocation			location;
	/** null means use {@link SpecialLocationEmitter#emitLocationHere(Location)} */
	private final ResolvableIntegralValue	valueOrNull;

	public AssembledSectionDeclaration(SectionDeclaration section)
	{
		this.location = new ExplicitSectionLocation(section.section());
		this.valueOrNull = section.value().map(ResolvableIntegralValue::new).orElse(null);
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		if(valueOrNull != null)
			valueOrNull.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		if(valueOrNull != null)
			locationEmitter.emitLocation(location, valueOrNull.resolvedValueOrZero());
		else
			locationEmitter.emitLocationHere(location);
	}
}
