package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.hasBigintMaxBitCountAndIsPositive;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValue;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledConstantOperand implements AssembledOperand
{
	private final IntegralValue value;

	private boolean wasAlwaysSmall;

	public AssembledConstantOperand(IntegralValue value)
	{
		this.value = value;

		this.wasAlwaysSmall = true;
	}

	@Override
	public boolean typeEncodeableOneBit(LabelResolver labelResolver)
	{
		return shouldEncodeAsSmallConstant(labelResolver);
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void append(SequentialMemoryWriteAccess codeSeq, LabelResolver labelResolver)
	{
		BigInteger value = value(labelResolver);
		if(shouldEncodeAsSmallConstant(labelResolver))
			codeSeq.writeNextByte(value.byteValue());
		else
			codeSeq.writeNextWord(bigintIntChecked(2, value, v -> "Immediate operand too large : " + v));
	}

	private boolean shouldEncodeAsSmallConstant(LabelResolver labelResolver)
	{
		boolean isCurrentlySmallConstant = isCurrentlySmallConstant(labelResolver);
		if(wasAlwaysSmall)
			return wasAlwaysSmall = isCurrentlySmallConstant;

		if(isCurrentlySmallConstant)
			System.err.println("WARNING: Operand was large, but became small - still encoding as large to ensure convergence");

		return false;
	}

	private boolean isCurrentlySmallConstant(LabelResolver labelResolver)
	{
		// The semantics of a small constant are "a value from 0-255".
		// So, we have to take care to exclude "negative" constants:
		// we want to force these to be a long constant starting with 0xff.
		// Otherwise, things like "add l0, -2" would behave very weirdly:
		// the -2 would fit into one byte and thus be represented as the small constant 0xfe,
		// which would not reduce l0 by 2, but increase it by 0xfe / 254.
		return hasBigintMaxBitCountAndIsPositive(8, value(labelResolver));
	}

	private BigInteger value(LabelResolver labelResolver)
	{
		return integralValue(value, labelResolver);
	}
}
