package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.MacroContext.GLOBAL_MACRO_CONTEXT;

import net.haspamelodica.javazmach.assembler.model.SectionDeclaration;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledSectionDeclaration implements AssembledEntry
{
	private final SpecialLocation			location;
	/** null means use {@link SpecialLocationEmitter#emitLocationHere(SpecialLocation)} */
	private final ResolvableIntegralValue	valueOrNull;

	public AssembledSectionDeclaration(SectionDeclaration section)
	{
		this.location = new ExplicitSectionLocation(section.section());
		// explicit section declarations are always in global context
		this.valueOrNull = section.value().map(v -> new ResolvableIntegralValue(GLOBAL_MACRO_CONTEXT, v)).orElse(null);
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
