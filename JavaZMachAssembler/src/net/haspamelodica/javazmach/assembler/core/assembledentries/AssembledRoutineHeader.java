package net.haspamelodica.javazmach.assembler.core.assembledentries;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue.resolvableIntValOrZero;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;

import java.math.BigInteger;
import java.util.List;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;
import net.haspamelodica.javazmach.assembler.core.valuereferences.LabelLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.Routine;
import net.haspamelodica.javazmach.assembler.model.values.LocalVariable;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledRoutineHeader implements AssembledEntry
{
	private final int macroRefId;

	private final boolean	writeInitialValues;
	private final int		alignmentBitCount;
	private final String	name;

	private static record AssembledLocalVariable(String name, ResolvableIntegralValue initialValue)
	{}
	private final List<AssembledLocalVariable> locals;

	public AssembledRoutineHeader(MacroContext macroContext, Routine routine, int version)
	{
		this.macroRefId = macroContext.refId();

		if(routine.locals().size() > 15)
			defaultError("More than 15 local variables declared");

		this.writeInitialValues = switch(version)
		{
			case 1, 2, 3, 4 -> true;
			case 5, 6, 7, 8 -> false;
			default -> throw new IllegalArgumentException("Unknown version: " + version);
		};
		// z1point1 §1.2.3
		this.alignmentBitCount = switch(version)
		{
			case 1, 2, 3 -> 1; // alignment 2
			case 4, 5 -> 2; // alignment 4
			case 6, 7 -> 2; // also alignment 4, although packed addresss are different here
			case 8 -> 3; // alignment 8
			default -> throw new IllegalArgumentException("Unknown version: " + version);
		};
		this.name = routine.name();
		this.locals = routine.locals().stream()
				.map(l -> new AssembledLocalVariable(l.name(), resolvableIntValOrZero(l.initialValue().map(macroContext::resolve))))
				.toList();
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		locals.forEach(r -> r.initialValue().updateResolvedValue(valueReferenceResolver));
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{

		memSeq.alignToBytes(1 << alignmentBitCount);
		locationEmitter.emitLocationHere(new LabelLocation(macroRefId, name), a -> a.shiftRight(alignmentBitCount));
		// no need to check whether this fits into a byte - locals count is checked in the constructor.
		memSeq.writeNextByte(locals.size());
		for(int localI = 0; localI < locals.size(); localI ++)
		{
			AssembledLocalVariable local = locals.get(localI);
			locationEmitter.emitLocation(new LabelLocation(macroRefId, local.name()), new LocalVariable(localI));
			BigInteger initialValue = local.initialValue().resolvedValueOrZero();
			if(writeInitialValues)
				memSeq.writeNextWord(bigintIntChecked(16, initialValue,
						i -> "Initial value for local variable " + local.name() + " too large: " + i, diagnosticHandler));
			else if(initialValue.signum() != 0)
				diagnosticHandler.error("Initial value for local variable " + local.name() + " is not 0: " + initialValue);
		}
	}
}
