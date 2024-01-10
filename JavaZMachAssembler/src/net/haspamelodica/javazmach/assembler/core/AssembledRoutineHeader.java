package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.math.BigInteger;
import java.util.List;

import net.haspamelodica.javazmach.assembler.model.NumberLiteral;
import net.haspamelodica.javazmach.assembler.model.Routine;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledRoutineHeader implements AssembledEntry
{
	private final boolean	writeInitialValues;
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
		this.name = routine.name();
		this.locals = routine.locals().stream()
				.map(l -> new AssembledLocalVariable(l.name(), new ResolvableIntegralValue(l.initialValue().orElse(new NumberLiteral(ZERO)))))
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
		memSeq.alignToBytes(2);
		locationEmitter.emitLocationHere(a -> a.shiftRight(1), new LabelLocation(name));
		// no need to check whether this fits into a byte - locals count is checked in the constructor.
		memSeq.writeNextByte(locals.size());
		for(AssembledLocalVariable local : locals)
		{
			BigInteger initialValue = local.initialValue().resolvedValueOrZero();
			if(writeInitialValues)
				memSeq.writeNextWord(ZAssemblerUtils.bigintIntChecked(16, initialValue,
						i -> "Initial value for local variable " + local.name() + " too large: " + i));
			else if(initialValue.signum() != 0)
				diagnosticHandler.error("Initial value for local variable " + local.name() + " is not 0: " + initialValue);
		}
	}
}
