package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public sealed interface AssembledEntry permits AssembledInstruction
{
	public void updateResolvedValues(LocationResolver locationResolver);
	public void append(SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler);
}
