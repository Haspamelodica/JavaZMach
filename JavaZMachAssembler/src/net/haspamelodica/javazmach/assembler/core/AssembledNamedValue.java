package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.assembler.model.NamedValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledNamedValue implements AssembledEntry
{
	private final int						macroRefId;
	private final String					name;
	private final ResolvableIntegralValue	value;

	public AssembledNamedValue(MacroContext macroContext, NamedValue namedValue)
	{
		this.macroRefId = macroContext.refId();
		this.name = namedValue.name();
		this.value = new ResolvableIntegralValue(macroContext, namedValue.value());
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		value.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocation(new LabelLocation(macroRefId, name), value.resolvedValueOrZero());
	}
}
