package net.haspamelodica.javazmach.assembler.core.assembledentries;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.MacroContext;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.LabelLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.NamedValue;
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
