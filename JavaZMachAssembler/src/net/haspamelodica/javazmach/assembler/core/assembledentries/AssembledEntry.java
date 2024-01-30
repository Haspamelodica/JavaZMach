package net.haspamelodica.javazmach.assembler.core.assembledentries;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public sealed interface AssembledEntry permits AssembledInstruction, AssembledRoutineHeader, AssembledLabelDeclaration,
		AssembledZObjectTable, AssembledGlobals, AssembledHeader, AssembledSectionDeclaration, AssembledDictionary,
		AssembledBuffer, AssembledNamedValue, AssembledAlignment
{
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver);
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler);
}
