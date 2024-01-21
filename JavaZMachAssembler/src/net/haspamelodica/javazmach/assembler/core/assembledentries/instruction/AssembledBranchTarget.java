package net.haspamelodica.javazmach.assembler.core.assembledentries.instruction;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.instruction.BranchLength;

public interface AssembledBranchTarget
{
	public void updateResolvedEncodedOffset(ValueReferenceResolver valueReferenceResolver);
	public BranchLength targetLength();
	public BigInteger getEncodedOffsetChecked(DiagnosticHandler diagnosticHandler);
}
