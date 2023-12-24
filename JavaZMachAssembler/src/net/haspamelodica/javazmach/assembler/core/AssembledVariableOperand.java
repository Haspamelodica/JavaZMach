package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.varnumByteAndUpdateRoutine;

import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public record AssembledVariableOperand(Variable variable) implements AssembledOperand
{
	@Override
	public boolean typeEncodeableOneBit(LabelResolver labelResolver)
	{
		return true;
	}

	@Override
	public int encodeTypeOneBitAssumePossible()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int encodeTypeTwoBits()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int sizeEstimate()
	{
		return 1;
	}

	@Override
	public void append(SequentialMemoryWriteAccess codeSeq, LabelResolver labelResolver)
	{
		codeSeq.writeNextByte(varnumByteAndUpdateRoutine(variable));
	}
}
