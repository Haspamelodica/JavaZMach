package net.haspamelodica.javazmach.assembler.core.assembledentries.dictionary;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

// Might be nicer as an interface
public sealed abstract class AssembledDictionaryDataElement permits AssembledDictionaryIntegralData, AssembledDictionaryByteSequenceData
{
	private final int size;

	public AssembledDictionaryDataElement(BigInteger size)
	{
		if(size.signum() < 0)
		{
			DiagnosticHandler.defaultError("Dictionary data entry cannot have negative size");
		}
		this.size = ZAssemblerUtils.bigintIntChecked(8, size, (b) -> "Data element size must not exceed 255! Got " + b.toString(), DiagnosticHandler.DEFAULT_HANDLER);
	}

	public int getSize()
	{
		return size;
	}

	public abstract void updateResolvedValues(ValueReferenceResolver valueReferenceResolver);
	public abstract void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler);

}
