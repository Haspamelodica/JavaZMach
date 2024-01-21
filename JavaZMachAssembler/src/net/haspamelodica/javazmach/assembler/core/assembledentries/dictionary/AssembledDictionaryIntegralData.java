package net.haspamelodica.javazmach.assembler.core.assembledentries.dictionary;

import static net.haspamelodica.javazmach.assembler.core.MacroContext.GLOBAL_MACRO_CONTEXT;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledDictionaryIntegralData extends AssembledDictionaryDataElement
{
	private BigInteger		resolvedValue;
	private IntegralValue	unresolvedValue;

	public AssembledDictionaryIntegralData(IntegralValue value, BigInteger size)
	{
		super(size);
		this.resolvedValue = null;
		this.unresolvedValue = value;
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		// dictionary is always in global context
		resolvedValue = ZAssemblerUtils.integralValueOrNull(GLOBAL_MACRO_CONTEXT, unresolvedValue, valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		int size = getSize();
		if(resolvedValue == null)
		{
			diagnosticHandler.error("Cannot resolve integral dictionary entry value");
			for(int i = 0; i < size; i ++)
			{
				memSeq.writeNextByte(0);
			}
		} else
		{
			byte bytes[] = ZAssemblerUtils.bigintBytesChecked(size * 8, resolvedValue, s -> "Dictionary integral data entry %s does not fit into declared size of %d bytes".formatted(resolvedValue, size), diagnosticHandler);
			byte prefixByte = resolvedValue.signum() < 0 ? (byte) -1 : (byte) 0;
			for(int i = 0; i < size - bytes.length; i ++)
			{
				// Prefix with sign bit to respect big endianness
				memSeq.writeNextByte(prefixByte);
			}
			memSeq.writeNextBytes(bytes);
		}
	}
}
