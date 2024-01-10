package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;

import java.math.BigInteger;
import java.util.List;

import net.haspamelodica.javazmach.assembler.model.Routine;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledRoutineHeader implements AssembledEntry
{
	private final boolean	writeInitialValues;
	private final int		alignmentBitCount;
	private final String	name;

	private static record AssembledLocalVariable(String name, ResolvableIntegralValue initialValue)
	{}
	private final List<AssembledLocalVariable> locals;

	public AssembledRoutineHeader(Routine routine, int version)
	{
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
				.map(l -> new AssembledLocalVariable(l.name(), new ResolvableIntegralValue(
						l.initialValue().map(AssemblerIntegralValue::intVal).orElse(intConst(0)))))
				.toList();
	}

	@Override
	public void updateResolvedValues(LocationResolver locationResolver)
	{
		locals.forEach(r -> r.initialValue().updateResolvedValue(locationResolver));
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		memSeq.alignToBytes(1 << alignmentBitCount);
		locationEmitter.emitLocationHere(new LabelLocation(name), a -> a.shiftRight(alignmentBitCount));
		// no need to check whether this fits into a byte - locals count is checked in the constructor.
		memSeq.writeNextByte(locals.size());
		for(AssembledLocalVariable local : locals)
		{
			BigInteger initialValue = local.initialValue().resolvedValueOrZero();
			if(writeInitialValues)
				memSeq.writeNextWord(bigintIntChecked(16, initialValue,
						i -> "Initial value for local variable " + local.name() + " too large: " + i, diagnosticHandler));
			else if(initialValue.signum() != 0)
				diagnosticHandler.error("Initial value for local variable " + local.name() + " is not 0: " + initialValue);
		}
	}
}
