package net.haspamelodica.javazmach.assembler.core.assembledentries;

import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.alignToBytes;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.materializeByteSequence;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.resolvableAlignmentValue;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue;
import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableCustomDefaultIntegralValue;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.Buffer;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledBuffer implements AssembledEntry
{
	private final AssembledIdentifierDeclaration		ident;
	private final ResolvableIntegralValue				byteLength;
	private final Optional<byte[]>						bytes;
	private final ResolvableCustomDefaultIntegralValue	alignment;

	public AssembledBuffer(MacroContext macroContext, Buffer buffer, int version, int packedAlignment)
	{
		this.ident = macroContext.resolve(buffer.ident());
		this.bytes = buffer.optSeq().map(b -> materializeByteSequence(b, version, s -> "Cannot compute buffer initial value: " + s));
		this.byteLength = new ResolvableIntegralValue(buffer.byteLength().map(macroContext::resolve).map(AssemblerIntegralValue::intVal)
				.orElseGet(() -> intConst(bytes.orElseGet(
						() -> defaultError("Buffer with neither initial content nor explicit length doesn't make sense")).length)));
		this.alignment = resolvableAlignmentValue(macroContext, buffer.alignment(), packedAlignment);
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		byteLength.updateResolvedValue(valueReferenceResolver);
		alignment.updateResolvedValue(valueReferenceResolver);
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		alignToBytes(memSeq, alignment, diagnosticHandler);
		locationEmitter.emitLocationHere(ident.asLabelLocation(), a -> a.divide(alignment.resolvedValueOrDefault()));
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
