package net.haspamelodica.javazmach.assembler.core.assembledentries.instruction;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReference;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReferenceIntegralOnly;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReferenceVariableOnly;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.IntegralReferredValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.VariableReferredValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledIntegralOrVariableOperand implements AssembledOperand
{
	private final ResolvedLabelReference	label;
	private final AssembledIntegralOperand	integralOperand;
	private final AssembledVariableOperand	variableOperand;

	private AssembledOperand currentOperand;

	public AssembledIntegralOrVariableOperand(ResolvedLabelReference label, boolean forcedSmallBecauseLONGForm)
	{
		this.label = label;
		this.integralOperand = new AssembledIntegralOperand(new ResolvedLabelReferenceIntegralOnly(label.macroContext(), label.name()), forcedSmallBecauseLONGForm);
		this.variableOperand = new AssembledVariableOperand(new ResolvedLabelReferenceVariableOnly(label.macroContext(), label.name()));

		currentOperand = null;
	}

	@Override
	public void updateResolvedValue(ValueReferenceResolver valueReferenceResolver)
	{
		//TODO not algorithmically nice: the label is resolved twice, and which type the label has is also checked twice.
		currentOperand = switch(label.macroContext().resolveLabel(label.name(), valueReferenceResolver))
		{
			// If the label doesn't exist (yet), just pick any - picking variable because it's always encodeable short.
			// In the first iteration, the resolved value doesn't matter at all,
			// and in subsequent iterations, the error diagnostic will have been emitted by resolveLabel already.
			case null -> variableOperand;
			case IntegralReferredValue v ->
			{
				integralOperand.updateResolvedValue(valueReferenceResolver);
				yield integralOperand;
			}
			case VariableReferredValue v ->
			{
				variableOperand.updateResolvedValue(valueReferenceResolver);
				yield variableOperand;
			}
		};
	}

	@Override
	public boolean typeEncodeableOneBit()
	{
		return currentOperand.typeEncodeableOneBit();
	}

	@Override
	public int encodeTypeOneBitAssumePossible()
	{
		return currentOperand.encodeTypeOneBitAssumePossible();
	}

	@Override
	public int encodeTypeTwoBits()
	{
		return currentOperand.encodeTypeTwoBits();
	}

	@Override
	public void append(SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		currentOperand.append(memSeq, diagnosticHandler);
	}
}
