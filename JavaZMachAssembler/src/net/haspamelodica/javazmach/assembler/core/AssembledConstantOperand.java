package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.hasBigintMaxBitCountAndIsPositive;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;

public final class AssembledConstantOperand implements AssembledOperand
{
	private final ValueAssembler	valueAssembler;
	private final IntegralValue		value;

	private boolean wasAlwaysSmall;

	public AssembledConstantOperand(ValueAssembler valueAssembler, IntegralValue value)
	{
		this.valueAssembler = valueAssembler;
		this.value = value;

		this.wasAlwaysSmall = true;
	}

	@Override
	public boolean typeEncodeableOneBit()
	{
		return shouldEncodeAsSmallConstant();
	}

	@Override
	public int encodeTypeOneBitAssumePossible()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int encodeTypeTwoBit()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean shouldEncodeAsSmallConstant()
	{
		boolean isCurrentlySmallConstant = isCurrentlySmallConstant();
		if(wasAlwaysSmall)
			return wasAlwaysSmall = isCurrentlySmallConstant;

		if(isCurrentlySmallConstant)
			System.err.println("WARNING: Operand was large, but became small - still encoding as large to ensure convergence");

		return false;
	}

	private boolean isCurrentlySmallConstant()
	{
		// The semantics of a small constant are "a value from 0-255".
		// So, we have to take care to exclude "negative" constants:
		// we want to force these to be a long constant starting with 0xff.
		// Otherwise, things like "add l0, -2" would behave very weirdly:
		// the -2 would fit into one byte and thus be represented as the small constant 0xfe,
		// which would not reduce l0 by 2, but increase it by 0xfe / 254.
		return hasBigintMaxBitCountAndIsPositive(8, valueAssembler.integralValue(value));
	}
}
