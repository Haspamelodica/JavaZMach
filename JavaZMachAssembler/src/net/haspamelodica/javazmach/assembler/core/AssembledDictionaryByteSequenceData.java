package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.ByteSequence;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledDictionaryByteSequenceData extends AssembledDictionaryDataElement
{
	private final ByteSequence seq;
	private final int version;

	public AssembledDictionaryByteSequenceData(ByteSequence seq, BigInteger size, int version)
	{
		super(size);
		this.version = version;
		this.seq = seq;
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		int size = getSize();
		byte bytes[] = ZAssemblerUtils.materializeByteSequence(seq, version, s -> "Cannot assemble dictionary byte sequence entry: " + s);
		byte bytesToWrite[];
		if(bytes.length != size)
		{
			if(bytes.length > size)
			{
				diagnosticHandler.error("Dictionary byte sequence entry is longer than declared. Declared bytes: %d; Actual bytes: %d".formatted(size, bytes.length));
			}
			bytesToWrite = new byte[size];
			System.arraycopy(bytes, 0, bytesToWrite, 0, Integer.min(size, bytes.length));
		} else
		{
			bytesToWrite = bytes;
		}
		memSeq.writeNextBytes(bytesToWrite);
	}

}
