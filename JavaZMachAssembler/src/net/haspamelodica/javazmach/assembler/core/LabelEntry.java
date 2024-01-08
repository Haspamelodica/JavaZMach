package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public record LabelEntry(String label) implements AssembledEntry
{
	@Override
	public void updateResolvedValues(LocationResolver locationsAndLabels)
	{
		// nothing to do
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler)
	{
		// nothing to append - only emit location
		locationEmitter.emitLocationHere(new LabelLocation(label));
	}
}
