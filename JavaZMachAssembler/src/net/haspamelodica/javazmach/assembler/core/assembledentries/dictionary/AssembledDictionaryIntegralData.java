package net.haspamelodica.javazmach.assembler.core.assembledentries.dictionary;

import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;
import static net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext.GLOBAL_MACRO_CONTEXT;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledDictionaryIntegralData extends AssembledDictionaryDataElement
{
	private final ResolvableIntegralValue value;

	public AssembledDictionaryIntegralData(IntegralValue value, BigInteger size)
	{
		super(size);
		// dictionary is always in global context
		this.value = new ResolvableIntegralValue(intVal(GLOBAL_MACRO_CONTEXT.resolve(value)));
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		value.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		int size = getSize();
		BigInteger resolvedValueOrZero = value.resolvedValueOrZero();

		byte bytes[] = ZAssemblerUtils.bigintBytesChecked(size * 8, resolvedValueOrZero,
				s -> "Dictionary integral data entry %s does not fit into declared size of %d bytes".formatted(resolvedValueOrZero, size),
				diagnosticHandler);
		byte prefixByte = resolvedValueOrZero.signum() < 0 ? (byte) -1 : (byte) 0;
		for(int i = 0; i < size - bytes.length; i ++)
		{
			// Prefix with sign bit to respect big endianness
			memSeq.writeNextByte(prefixByte);
		}
		memSeq.writeNextBytes(bytes);
	}
}
