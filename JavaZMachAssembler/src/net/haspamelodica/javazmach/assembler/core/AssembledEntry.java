package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public interface AssembledEntry
{
	public void updateResolvedValues(LocationResolver locationsAndLabels);
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler);
}
