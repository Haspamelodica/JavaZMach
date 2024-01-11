package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.memory.WritableMemory;

public interface AssembledIntegralHeaderField
{
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver);
	public void assemble(WritableMemory header, DiagnosticHandler diagnosticHandler);
	public HeaderField getField();
}
