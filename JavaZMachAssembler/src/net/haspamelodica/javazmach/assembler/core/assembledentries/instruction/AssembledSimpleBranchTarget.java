package net.haspamelodica.javazmach.assembler.core.assembledentries.instruction;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.model.entries.instruction.BranchLength.SHORTBRANCH;

import java.math.BigInteger;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.instruction.BranchLength;
import net.haspamelodica.javazmach.assembler.model.values.SimpleBranchTarget;

public record AssembledSimpleBranchTarget(SimpleBranchTarget target, Optional<BranchLength> branchLengthOverride) implements AssembledBranchTarget
{
	@Override
	public void updateResolvedEncodedOffset(ValueReferenceResolver valueReferenceResolver)
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
