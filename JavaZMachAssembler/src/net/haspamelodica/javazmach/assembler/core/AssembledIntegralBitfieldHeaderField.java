package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;
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
		this.value = new ResolvableIntegralValue(value);
	}

	@Override
	public void updateResolvedValues(LocationResolver locationResolver)
	{
		value.updateResolvedValue(locationResolver);
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
