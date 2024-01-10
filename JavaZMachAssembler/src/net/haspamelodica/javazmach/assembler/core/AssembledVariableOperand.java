package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.varnumByteAndUpdateRoutine;

import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public record AssembledVariableOperand(Variable variable) implements AssembledOperand
{
	@Override
	public void updateResolvedValue(LocationResolver locationResolver)
	{}

	@Override
	public boolean typeEncodeableOneBit()
	{
		return true;
	}

	@Override
	public int encodeTypeOneBitAssumePossible()
	{
		return 1;
	}

	@Override
	public int encodeTypeTwoBits()
	{
		return 0b10;
	}

	@Override
	public void append(SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		memSeq.writeNextByte(varnumByteAndUpdateRoutine(variable));
	}
}
