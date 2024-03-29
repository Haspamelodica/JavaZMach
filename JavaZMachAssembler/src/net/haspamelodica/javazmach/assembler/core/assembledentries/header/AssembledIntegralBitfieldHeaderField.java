package net.haspamelodica.javazmach.assembler.core.assembledentries.header;

import static net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext.GLOBAL_MACRO_CONTEXT;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.WritableMemory;

public class AssembledIntegralBitfieldHeaderField implements AssembledIntegralHeaderField
{
	private final HeaderField				field;
	private final ResolvableIntegralValue	value;

	public AssembledIntegralBitfieldHeaderField(HeaderField field, IntegralValue value)
	{
		this.field = field;
		// header is always in global context
		this.value = new ResolvableIntegralValue(GLOBAL_MACRO_CONTEXT.resolve(value));
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		value.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void assemble(WritableMemory header, DiagnosticHandler diagnosticHandler)
	{
		BigInteger resolvedValue = value.resolvedValueOrZero();
		if(resolvedValue.signum() != 0 && !resolvedValue.equals(BigInteger.ONE))
			diagnosticHandler.error("Value of bitfield entry is neither 0 nor 1: field "
					+ field + ", value " + resolvedValue);

		HeaderParser.setFieldUnchecked(header, field, resolvedValue.signum());
	}

	@Override
	public HeaderField getField()
	{
		return field;
	}
}
