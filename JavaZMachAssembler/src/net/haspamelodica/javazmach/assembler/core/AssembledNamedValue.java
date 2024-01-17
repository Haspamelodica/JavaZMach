package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.assembler.model.NamedValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledNamedValue implements AssembledEntry
{
	private final String					name;
	private final ResolvableIntegralValue	value;

	public AssembledNamedValue(NamedValue namedValue)
	{
		this.name = namedValue.name();
		this.value = new ResolvableIntegralValue(namedValue.value());
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		value.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocation(new LabelLocation(name), value.resolvedValueOrZero());
	}
}
