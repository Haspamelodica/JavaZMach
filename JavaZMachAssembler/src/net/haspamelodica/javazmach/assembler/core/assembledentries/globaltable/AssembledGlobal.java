package net.haspamelodica.javazmach.assembler.core.assembledentries.globaltable;

import static net.haspamelodica.javazmach.assembler.core.MacroContext.GLOBAL_MACRO_CONTEXT;
import static net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue.resolvableIntValOrZero;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.globaltable.Global;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledGlobal
{
	private final ResolvableIntegralValue defaultValue;

	public AssembledGlobal(Global global)
	{
		// globals are always in global context
		this.defaultValue = resolvableIntValOrZero(GLOBAL_MACRO_CONTEXT, global.initialValue());
	}

	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		defaultValue.updateResolvedValue(valueReferenceResolver);
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		memSeq.writeNextWord(bigintIntChecked(16, defaultValue.resolvedValueOrZero(),
				(b) -> "Default value %s too large for global. Must be at most 2 bytes".formatted(b), diagnosticHandler));
	}
}
