package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.materializeByteSequence;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.Buffer;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledBuffer implements AssembledEntry
{
	private final String					name;
	private final ResolvableIntegralValue	byteLength;
	private final Optional<byte[]>			bytes;

	public AssembledBuffer(Buffer buffer, int version)
	{
		this.name = buffer.name();
		this.byteLength = new ResolvableIntegralValue(buffer.byteLength());
		this.bytes = buffer.optSeq().map(b -> materializeByteSequence(b, version, s -> "Cannot compute buffer initial value: " + s));
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		byteLength.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocationHere(new LabelLocation(name));
		int byteLengthInt = bigintIntChecked(31, byteLength.resolvedValueOrZero(), b -> "Buffer size %s is too large!".formatted(b), diagnosticHandler);

		int written = 0;
		if(bytes.isPresent())
		{
			byte[] bytes = this.bytes.get();
			if(bytes.length > byteLengthInt)
				diagnosticHandler.error("Buffer initializer too long. Expected %d bytes, got %d".formatted(byteLength, bytes.length));
			memSeq.writeNextBytes(bytes);
			written = bytes.length;
		}
		for(int i = written; i < byteLengthInt; i ++)
		{
			memSeq.writeNextByte(0);
		}
	}

}
