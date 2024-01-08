package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public sealed interface AssembledOperand permits AssembledImmediateOperand, AssembledVariableOperand
{
	public void updateResolvedValue(LocationResolver locationResolver);
	public boolean typeEncodeableOneBit();
	public int encodeTypeOneBitAssumePossible();
	public int encodeTypeTwoBits();
	public void append(SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler);
}
