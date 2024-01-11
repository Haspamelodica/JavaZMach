package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue.resolvableIntValOrZero;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;

import net.haspamelodica.javazmach.assembler.model.Global;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledGlobal
{
	private final ResolvableIntegralValue defaultValue;

	public AssembledGlobal(Global global)
	{
		this.defaultValue = resolvableIntValOrZero(global.initialValue());
	}

	public void updateResolvedValues(LocationResolver locationResolver)
	{
		defaultValue.updateResolvedValue(locationResolver);
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		memSeq.writeNextWord(bigintIntChecked(16, defaultValue.resolvedValueOrZero(),
				(b) -> "Default value %s too large for global. Must be at most 2 bytes".formatted(b), diagnosticHandler));
	}
}
