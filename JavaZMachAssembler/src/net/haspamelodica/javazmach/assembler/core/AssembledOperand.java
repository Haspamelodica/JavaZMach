package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public sealed interface AssembledOperand permits AssembledConstantOperand, AssembledVariableOperand
{
	public boolean typeEncodeableOneBit(LabelResolver labelResolver);
	public int encodeTypeOneBitAssumePossible();
	public int encodeTypeTwoBits();
	public int sizeEstimate();
	public void append(SequentialMemoryWriteAccess codeSeq, LabelResolver labelResolver);
}
