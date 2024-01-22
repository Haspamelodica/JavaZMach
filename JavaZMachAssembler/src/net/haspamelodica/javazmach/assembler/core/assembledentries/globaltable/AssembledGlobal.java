package net.haspamelodica.javazmach.assembler.core.assembledentries.globaltable;

import static net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue.resolvableIntValOrZero;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext.GLOBAL_MACRO_CONTEXT;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.LabelLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.globaltable.Global;
import net.haspamelodica.javazmach.assembler.model.values.GlobalVariable;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledGlobal
{
	private final String					name;
	private final ResolvableIntegralValue	defaultValue;

	public AssembledGlobal(Global global)
	{
		this.name = global.name();
		// globals are always in global context
		this.defaultValue = resolvableIntValOrZero(global.initialValue().map(GLOBAL_MACRO_CONTEXT::resolve));
	}

	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		defaultValue.updateResolvedValue(valueReferenceResolver);
	}

	public void append(int index, SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		// globals are always in global context
		locationEmitter.emitLocation(new LabelLocation(GLOBAL_MACRO_CONTEXT.refId(), name), new GlobalVariable(index));
		memSeq.writeNextWord(bigintIntChecked(16, defaultValue.resolvedValueOrZero(),
				(b) -> "Default value %s too large for global. Must be at most 2 bytes".formatted(b), diagnosticHandler));
	}
}
