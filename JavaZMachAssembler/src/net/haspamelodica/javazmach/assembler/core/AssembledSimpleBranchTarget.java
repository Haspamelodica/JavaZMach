package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.model.BranchLength.SHORTBRANCH;

import java.math.BigInteger;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.BranchLength;
import net.haspamelodica.javazmach.assembler.model.SimpleBranchTarget;

public record AssembledSimpleBranchTarget(SimpleBranchTarget target, Optional<BranchLength> branchLengthOverride) implements AssembledBranchTarget
{
	@Override
	public void updateResolvedEncodedOffset(LocationAndLabelResolver locationResolver)
	{}

	@Override
	public BranchLength targetLength()
	{
		return branchLengthOverride.orElse(SHORTBRANCH);
	}

	@Override
	public BigInteger getEncodedOffsetChecked(DiagnosticHandler diagnosticHandler)
	{
		return switch(target)
		{
			case rfalse -> ZERO;
			case rtrue -> ONE;
		};
	}
}
