package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.TWO;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValueOrNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.BranchLength;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;

public class AssembledRegularBranchTarget implements AssembledBranchTarget
{
	private final SizeVariantAssemblerUnsigned<BranchLength> encodedOffsetAssembler;

	public AssembledRegularBranchTarget(IntegralValue target, Location branchOriginLocation, Optional<BranchLength> branchLengthOverride)
	{
		this.encodedOffsetAssembler = new SizeVariantAssemblerUnsigned<>(
				locationResolver ->
				{
					BigInteger resolvedTarget = integralValueOrNull(target, locationResolver);
					if(resolvedTarget == null)
						return null;
					BigInteger resolvedBranchOrigin = locationResolver.resolveAbsoluteOrNull(branchOriginLocation);
					if(resolvedBranchOrigin == null)
						return null;
					return resolvedTarget.subtract(resolvedBranchOrigin).add(TWO);
				},
				// this depends on BranchLength being sorted in ascending length
				Arrays.asList(BranchLength.values()),
				branchLengthOverride, l -> switch(l)
				{
					case SHORTBRANCH -> 6;
					case LONGBRANCH -> 14;
				}, l -> switch(l)
				{
					case SHORTBRANCH -> false;
					case LONGBRANCH -> true;
				});
	}

	@Override
	public void updateResolvedEncodedOffset(LocationAndLabelResolver locationResolver)
	{
		encodedOffsetAssembler.update(locationResolver);
	}

	@Override
	public BranchLength targetLength()
	{
		return encodedOffsetAssembler.getTargetSizeUnchecked();
	}

	@Override
	public BigInteger getEncodedOffsetChecked(DiagnosticHandler diagnosticHandler)
	{
		BigInteger encodedOffset = encodedOffsetAssembler.getResolvedValueChecked(
				v -> "Branch offset too long: " + v.subtract(TWO),
				v -> "Branch offset too long for branch length " + encodedOffsetAssembler.getTargetSizeUnchecked() + ": " + v.subtract(TWO),
				v -> "Branch offset was long, but became short - still encoding as long to ensure convergence: " + v.subtract(TWO),
				diagnosticHandler);
		if(encodedOffset.equals(BigInteger.ZERO) || encodedOffset.equals(BigInteger.ONE))
			diagnosticHandler.error("A branch offset of " + encodedOffset.subtract(TWO)
					+ " is not encodable as it would conflict with rtrue / rfalse");
		return encodedOffset;
	}
}
