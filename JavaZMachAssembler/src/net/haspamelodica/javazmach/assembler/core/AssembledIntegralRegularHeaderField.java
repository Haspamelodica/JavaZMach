package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintBytesChecked;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValueOrNull;

import java.math.BigInteger;
import java.util.Arrays;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.WritableMemory;

public class AssembledIntegralRegularHeaderField implements AssembledIntegralHeaderField
{
	private final HeaderField	field;
	private final IntegralValue	value;

	public AssembledIntegralRegularHeaderField(HeaderField field, IntegralValue value)
	{
		this.field = field;
		this.value = value;
	}

	@Override
	public void assemble(WritableMemory header, LocationResolver locationResolver)
	{
		BigInteger resolvedValue = integralValueOrNull(value, locationResolver);
		byte[] valueBytes = bigintBytesChecked(field.len * 8, resolvedValue, bigint -> "header field value out of range: "
				+ bigint + " for field " + field);
		int padding = field.len - valueBytes.length;
		if(padding != 0)
		{
			byte[] valueBytesOrig = valueBytes;
			valueBytes = new byte[field.len];
			System.arraycopy(valueBytesOrig, 0, valueBytes, padding, valueBytesOrig.length);
			if(resolvedValue.signum() < 0)
				Arrays.fill(valueBytes, 0, padding, (byte) -1);
		}
		HeaderParser.setFieldUncheckedBytes(header, field, valueBytes, 0, field.len);
	}

	@Override
	public HeaderField getField()
	{
		return field;
	}
}
