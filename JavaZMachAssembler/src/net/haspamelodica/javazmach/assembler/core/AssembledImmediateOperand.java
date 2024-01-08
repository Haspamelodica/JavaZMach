package net.haspamelodica.javazmach.assembler.core;

import java.util.List;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledImmediateOperand implements AssembledOperand
{
	private final SizeVariantAssemblerUnsigned<Boolean> value;

	public AssembledImmediateOperand(IntegralValue value, boolean forcedSmallBecauseLONGForm)
	{
		this.value = new SizeVariantAssemblerUnsigned<>(value, List.of(true, false),
				forcedSmallBecauseLONGForm ? Optional.of(true) : Optional.empty(),
				// The semantics of a small immediate are "a value from 0-255".
				// So, the small size has to be unsigned to exclude "negative" immediates:
				// we want to force these to be a long immediate starting with 0xff.
				// Otherwise, things like "add l0, -2" would behave very weirdly:
				// the -2 would fit into one byte and thus be represented as the small immediate 0xfe,
				// which would not reduce l0 by 2, but increase it by 0xfe / 254.
				small -> small ? 8 : 16, small -> !small);
	}

	@Override
	public void updateResolvedValue(LocationAndLabelResolver locationResolver)
	{
		value.update(locationResolver);
	}

	@Override
	public boolean typeEncodeableOneBit()
	{
		return targetingSmallSize();
	}

	@Override
	public int encodeTypeOneBitAssumePossible()
	{
		return 0;
	}

	@Override
	public int encodeTypeTwoBits()
	{
		return targetingSmallSize() ? 0b01 : 0b00;
	}

	@Override
	public void append(SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler)
	{
		if(targetingSmallSize())
			codeSeq.writeNextByte(valueChecked(diagnosticHandler));
		else
			codeSeq.writeNextWord(valueChecked(diagnosticHandler));
	}

	private int valueChecked(DiagnosticHandler diagnosticHandler)
	{
		return value.getResolvedValueChecked(
				v -> "Immediate too large: " + v,
				v -> "Immediate too large for LONG form: " + v,
				v -> "Immediate was large, but became small - still encoding as large to ensure convergence: " + v,
				diagnosticHandler).intValue();
	}

	private boolean targetingSmallSize()
	{
		return value.getTargetSizeUnchecked();
	}
}
