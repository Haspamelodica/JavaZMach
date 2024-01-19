package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;
import static net.haspamelodica.javazmach.assembler.core.MacroContext.GLOBAL_MACRO_CONTEXT;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintBytesChecked;

import java.math.BigInteger;
import java.util.Arrays;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.WritableMemory;

public class AssembledIntegralRegularHeaderField implements AssembledIntegralHeaderField
{
	private final HeaderField				field;
	private final ResolvableIntegralValue	value;

	public AssembledIntegralRegularHeaderField(HeaderField field, IntegralValue value)
	{
		// header fields are always in global context
		this(field, intVal(GLOBAL_MACRO_CONTEXT, value));
	}
	public AssembledIntegralRegularHeaderField(HeaderField field, AssemblerIntegralValue value)
	{
		this.field = field;
		this.value = new ResolvableIntegralValue(value);
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
		byte[] valueBytes = bigintBytesChecked(field.len * 8, resolvedValue, bigint -> "header field value out of range: "
				+ bigint + " for field " + field, diagnosticHandler);
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
