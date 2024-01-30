package net.haspamelodica.javazmach.assembler.core.assembledentries;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.alignToBytes;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.resolvableAlignmentValue;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableCustomDefaultIntegralValue;
import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.LabelDeclaration;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledLabelDeclaration implements AssembledEntry
{
	private final AssembledIdentifierDeclaration		ident;
	private final ResolvableCustomDefaultIntegralValue	alignment;

	public AssembledLabelDeclaration(MacroContext macroContext, LabelDeclaration labelDeclaration, int packedAlignment)
	{
		this.ident = macroContext.resolve(labelDeclaration.ident());
		this.alignment = resolvableAlignmentValue(macroContext, labelDeclaration.alignment(), packedAlignment);
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
		locationEmitter.emitLocationHere(ident.asLabelLocation(), l -> l.divide(alignment.resolvedValueOrDefault()));
	}
}
