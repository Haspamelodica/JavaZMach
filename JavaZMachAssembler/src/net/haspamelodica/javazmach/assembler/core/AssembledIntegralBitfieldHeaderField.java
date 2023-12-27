package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValueOrNull;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.WritableMemory;

public class AssembledIntegralBitfieldHeaderField implements AssembledIntegralHeaderField
{
	private final HeaderField	field;
	private final IntegralValue	value;

	public AssembledIntegralBitfieldHeaderField(HeaderField field, IntegralValue value)
	{
		this.field = field;
		this.value = value;
	}

	@Override
	public void assemble(WritableMemory header, LocationResolver locationResolver)
	{
		BigInteger resolvedValue = integralValueOrNull(value, locationResolver);
		if(resolvedValue.signum() != 0 && !resolvedValue.equals(BigInteger.ONE))
			defaultError("Value of bitfield entry is neither 0 nor 1: field "
					+ field + ", value " + resolvedValue);

		HeaderParser.setFieldUnchecked(header, field, resolvedValue.testBit(0) ? 1 : 0);
	}

	@Override
	public HeaderField getField()
	{
		return field;
	}
}
