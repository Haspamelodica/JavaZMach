package net.haspamelodica.javazmach.assembler.core.assembledentries.instruction;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.varnumByte;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableVariable;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedVariable;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledVariableOperand implements AssembledOperand
{
	private final ResolvableVariable variable;

	public AssembledVariableOperand(ResolvedVariable variable)
	{
		this.variable = new ResolvableVariable(variable);
	}

	@Override
	public void updateResolvedValue(ValueReferenceResolver valueReferenceResolver)
	{
		variable.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public boolean typeEncodeableOneBit()
	{
		return true;
	}

	@Override
	public int encodeTypeOneBitAssumePossible()
	{
		return 1;
	}

	@Override
	public int encodeTypeTwoBits()
	{
		return 0b10;
	}

	@Override
	public void append(SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		memSeq.writeNextByte(varnumByte(variable.resolvedVariableOrSp()));
	}
}
