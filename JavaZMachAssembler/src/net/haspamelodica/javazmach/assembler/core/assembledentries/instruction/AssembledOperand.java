package net.haspamelodica.javazmach.assembler.core.assembledentries.instruction;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public sealed interface AssembledOperand permits AssembledImmediateOperand, AssembledVariableOperand
{
	public void updateResolvedValue(ValueReferenceResolver valueReferenceResolver);
	public boolean typeEncodeableOneBit();
	public int encodeTypeOneBitAssumePossible();
	public int encodeTypeTwoBits();
	public void append(SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler);
}
