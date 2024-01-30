package net.haspamelodica.javazmach.assembler.core.assembledentries;

import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.Alignment;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.values.SimpleAlignmentValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledAlignment implements AssembledEntry
{
	private final ResolvableIntegralValue alignment;

	public AssembledAlignment(MacroContext macroContext, Alignment alignment, int packedAlignment)
	{
		this.alignment = new ResolvableIntegralValue(switch(alignment.alignment())
		{
			case IntegralValue a -> intVal(macroContext.resolve(a));
			case SimpleAlignmentValue a -> intConst(packedAlignment);
		});
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		alignment.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		memSeq.alignToBytes(bigintIntChecked(31, alignment.resolvedValueOrZero(),
				a -> "Alignment doesn't fit in int: " + a, diagnosticHandler));
	}
}
