package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.BranchLength;

public interface AssembledBranchTarget
{
	public void updateResolvedEncodedOffset(ValueReferenceResolver valueReferenceResolver);
	public BranchLength targetLength();
	public BigInteger getEncodedOffsetChecked(DiagnosticHandler diagnosticHandler);
}
