package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public sealed interface AssembledEntry permits AssembledInstruction
{
	public void updateResolvedValues(LocationAndLabelResolver locationsAndLabels);
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler);
}
