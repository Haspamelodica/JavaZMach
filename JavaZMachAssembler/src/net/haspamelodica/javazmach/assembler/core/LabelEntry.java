package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public record LabelEntry(String label) implements AssembledEntry
{
	@Override
	public void updateResolvedValues(LocationResolver locationResolver)
	{
		// nothing to do
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		// nothing to append - only emit location
		locationEmitter.emitLocationHere(new LabelLocation(label));
	}
}
