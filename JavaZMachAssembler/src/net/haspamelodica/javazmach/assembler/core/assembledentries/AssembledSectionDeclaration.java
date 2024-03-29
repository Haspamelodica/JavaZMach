package net.haspamelodica.javazmach.assembler.core.assembledentries;

import static net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext.GLOBAL_MACRO_CONTEXT;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.ExplicitSectionLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.SpecialLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.SectionDeclaration;
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
		this.valueOrNull = section.value().map(v -> new ResolvableIntegralValue(GLOBAL_MACRO_CONTEXT.resolve(v))).orElse(null);
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
