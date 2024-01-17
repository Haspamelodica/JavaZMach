package net.haspamelodica.javazmach.assembler.core;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.Buffer;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledBuffer implements AssembledEntry
{
	private final String			name;
	private final int				byteLength;
	private final Optional<byte[]>	bytes;

	public AssembledBuffer(Buffer buffer, int version)
	{
		name = buffer.name();
		byteLength = ZAssemblerUtils.bigintIntChecked(31, buffer.byteLength(), b -> "Buffer size %s is too large!".formatted(b), DiagnosticHandler.DEFAULT_HANDLER);
		if(buffer.optSeq().isPresent())
		{
			byte[] bytes = ZAssemblerUtils.materializeByteSequence(buffer.optSeq().get(), version, s -> "Cannot compute buffer default value: " + s);
			if(bytes.length > byteLength)
			{
				DiagnosticHandler.defaultError("Buffer initializer too long. Expected %d bytes, got %d".formatted(byteLength, bytes.length));
			}
			this.bytes = Optional.of(bytes);
		} else
		{
			this.bytes = Optional.empty();
		}
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocationHere(new LabelLocation(name));
		int written = 0;
		if(bytes.isPresent())
		{
			byte[] bytes = this.bytes.get();
			memSeq.writeNextBytes(bytes);
			written = bytes.length;
		}
		for(int i = written; i < byteLength; i ++)
		{
			memSeq.writeNextByte(0);
		}
	}

}
