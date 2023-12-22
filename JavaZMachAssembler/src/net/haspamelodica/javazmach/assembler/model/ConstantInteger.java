package net.haspamelodica.javazmach.assembler.model;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.hasBigintMaxByteCount;

import java.math.BigInteger;

public record ConstantInteger(BigInteger value) implements Operand, HeaderValue, BranchTarget, ConstantByteSequenceElement
{
	@Override
	public boolean isTypeEncodeableUsingOneBit()
	{
		return isSmallConstant();
	}

	@Override
	public int encodeTypeOneBit()
	{
		return 0;
	}

	@Override
	public int encodeTypeTwoBits()
	{
		return isSmallConstant() ? 0b01 : 0b00;
	}

	public boolean isSmallConstant()
	{
		// The semantics of a small constant are "a value from 0-255".
		// So, we have to take care to exclude "negative" constants:
		// we want to force these to be a long constant starting with 0xff.
		// Otherwise, things like "add l0, -2" would behave very weirdly:
		// the -2 would fit into one byte and thus be represented as the small constant 0xfe,
		// which would not reduce l0 by 2, but increase it by 0xfe / 254.
		return value.signum() >= 0 && hasBigintMaxByteCount(1, value);
	}
}
