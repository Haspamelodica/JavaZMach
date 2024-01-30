package net.haspamelodica.javazmach.assembler.core.assembledentries;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.alignToBytes;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.resolvableAlignmentValue;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableCustomDefaultIntegralValue;
import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.Alignment;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledAlignment implements AssembledEntry
{
	private final ResolvableCustomDefaultIntegralValue alignment;

	public AssembledAlignment(MacroContext macroContext, Alignment alignment, int packedAlignment)
	{
		this.alignment = resolvableAlignmentValue(macroContext, alignment.alignment(), packedAlignment);
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		alignment.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		alignToBytes(memSeq, alignment, diagnosticHandler);
	}
}
