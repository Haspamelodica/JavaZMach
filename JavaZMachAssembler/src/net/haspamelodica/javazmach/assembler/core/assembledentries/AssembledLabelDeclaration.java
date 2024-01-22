package net.haspamelodica.javazmach.assembler.core.assembledentries;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.LabelDeclaration;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledLabelDeclaration implements AssembledEntry
{
	private final AssembledIdentifierDeclaration ident;

	public AssembledLabelDeclaration(MacroContext macroContext, LabelDeclaration labelDeclaration)
	{
		this.ident = macroContext.resolve(labelDeclaration.ident());
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		// nothing to do
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		// nothing to append - only emit location
		locationEmitter.emitLocationHere(ident.asLabelLocation());
	}
}
