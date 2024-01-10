package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public sealed interface AssembledEntry permits AssembledInstruction, AssembledRoutineHeader, LabelEntry, AssembledZObjectTable, AssembledGlobals
{
	public void updateResolvedValues(LocationResolver locationResolver);
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler);
}
