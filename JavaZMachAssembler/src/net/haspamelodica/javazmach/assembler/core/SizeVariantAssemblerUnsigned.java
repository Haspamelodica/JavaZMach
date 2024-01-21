package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.hasBigintMaxBitCount;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.hasBigintMaxBitCountAndIsPositive;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;

// TODO better name
public final class SizeVariantAssemblerUnsigned<SIZE>
{
	private final AssemblerIntegralValue value;

	private final List<SIZE>	allSizes;
	private final boolean		sizeOverridden;

	private final ToIntFunction<SIZE>	sizeToBitcount;
	private final Predicate<SIZE>		sizeToSignedness;

	private BigInteger	resolvedValue;
	private SIZE		targetSize;
	private SIZE		currentSize;

	public SizeVariantAssemblerUnsigned(AssemblerIntegralValue value, List<SIZE> allSizes,
			Optional<SIZE> sizeOverride, ToIntFunction<SIZE> sizeToBitcount, Predicate<SIZE> sizeToSignedness)
	{
		this.value = value;
		this.allSizes = List.copyOf(allSizes);
		this.sizeOverridden = sizeOverride.isPresent();
		this.sizeToBitcount = sizeToBitcount;
		this.sizeToSignedness = sizeToSignedness;

		// if not overridden, choose smallest size to start
		this.targetSize = sizeOverride.orElse(allSizes.get(0));
	}

	public void update(ValueReferenceResolver valueReferenceResolver)
	{
		resolvedValue = value.resolve(valueReferenceResolver);
		if(resolvedValue == null)
			// nothing about value known yet - continue assuming it is the smallest size / the overridden size.
			return;

		currentSize = computeCurrentSize();
		if(currentSize == null)
			// The currently resolved value is bogus, so at the moment we can't assemble anyway.
			// So, we can safely skip updating targetSize.
			return;

		if(sizeOverridden)
			// don't update target size if overridden
			return;

		// Only ever increase target size to ensure convergence.
		// The corresponding warning is emitted in checkAndGetTargetSize() - value might become larger again later.
		int targetBitcount = sizeToBitcount.applyAsInt(targetSize);
		if(sizeToBitcount.applyAsInt(currentSize) > targetBitcount)
			targetSize = currentSize;

		resolvedValue = resolvedValue.and(BigInteger.valueOf((1 << targetBitcount) - 1));
	}

	private SIZE computeCurrentSize()
	{
		for(int i = 0; i < allSizes.size(); i ++)
		{
			SIZE size = allSizes.get(i);
			int sizeBitcount = sizeToBitcount.applyAsInt(size);
			boolean fitsSize = sizeToSignedness.test(size)
					? hasBigintMaxBitCount(sizeBitcount, resolvedValue)
					: hasBigintMaxBitCountAndIsPositive(sizeBitcount, resolvedValue);
			if(fitsSize)
				return size;
		}
		return null;
	}

	public SIZE getTargetSizeUnchecked()
	{
		return targetSize;
	}

	public BigInteger getResolvedValueChecked(
			Function<BigInteger, String> generallyTooLargeErrorMessage,
			Function<BigInteger, String> tooLargeForOverrideErrorMessage,
			Function<BigInteger, String> valueBecameSmallerInfoMessage,
			DiagnosticHandler diagnosticHandler)
	{
		check(generallyTooLargeErrorMessage, tooLargeForOverrideErrorMessage, valueBecameSmallerInfoMessage, diagnosticHandler);

		// Make sure that the caller gets some value even if we don't have one yet -
		// important for the first assembler iteration where some locations aren't known yet and where the diagnostic handler ignores errors.
		return resolvedValue == null ? ZERO : resolvedValue;
	}

	private void check(
			Function<BigInteger, String> generallyTooLargeErrorMessage,
			Function<BigInteger, String> tooLargeForOverrideErrorMessage,
			Function<BigInteger, String> valueBecameSmallerInfoMessage,
			DiagnosticHandler diagnosticHandler)
	{
		if(resolvedValue == null)
		{
			diagnosticHandler.error("No value known yet, but asked for value - this is an interpreter bug.");
			return;
		}
		if(currentSize == null)
		{
			diagnosticHandler.error(generallyTooLargeErrorMessage.apply(resolvedValue));
			return;
		}

		int currentSizeBitcount = sizeToBitcount.applyAsInt(currentSize);
		int targetSizeBitcount = sizeToBitcount.applyAsInt(targetSize);

		if(sizeOverridden)
		{
			if(currentSizeBitcount > targetSizeBitcount)
				diagnosticHandler.error(tooLargeForOverrideErrorMessage.apply(resolvedValue));
		} else
		{
			if(currentSizeBitcount < targetSizeBitcount)
				diagnosticHandler.info(valueBecameSmallerInfoMessage.apply(resolvedValue));
		}
	}
}
