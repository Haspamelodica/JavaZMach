package net.haspamelodica.javazmach.assembler.core.assembledentries;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.MacroContext;
import net.haspamelodica.javazmach.assembler.core.valuereferences.LabelLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public record AssembledLabelDeclaration(MacroContext macroContext, String label) implements AssembledEntry
{
	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		// nothing to do
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		// nothing to append - only emit location
		locationEmitter.emitLocationHere(new LabelLocation(macroContext.refId(), label));
	}
}
