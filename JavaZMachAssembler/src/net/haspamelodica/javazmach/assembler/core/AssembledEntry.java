package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public sealed interface AssembledEntry permits AssembledInstruction, AssembledRoutineHeader, AssembledLabelDeclaration, AssembledZObjectTable, AssembledGlobals, AssembledHeader, AssembledSectionDeclaration
{
	public void updateResolvedValues(LocationResolver locationResolver);
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler);
}
