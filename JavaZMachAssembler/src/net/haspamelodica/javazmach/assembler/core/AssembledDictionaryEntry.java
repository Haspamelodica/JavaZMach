package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.haspamelodica.javazmach.assembler.model.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.DictionaryDataElement;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.ZString;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledDictionaryEntry
{
	private final static int						KEY_LENGTH_V1_TO_V3			= 4;
	private final static int						KEY_LENGTH_V4_TO_V6			= 6;
	private final static int						KEY_ZCHAR_LENGTH_V1_TO_V3	= 6;
	private final static int						KEY_ZCHAR_LENGTH_V4_TO_V6	= 9;
	private final List<Byte>						keyZChars;
	private List<AssembledDictionaryDataElement>	elements;
	private int										totalSize;

	public AssembledDictionaryEntry(ZString key, List<DictionaryDataElement> entries, int version)
	{
		this.elements = entries.stream().map(e ->
		{
			return switch(e.value())
			{
				case IntegralValue value -> new AssembledDictionaryIntegralData(value, e.size());
				case ByteSequence value -> new AssembledDictionaryByteSequenceData(value, e.size(), version);
			};
		}).toList();
		int zCharLength;
		if(version >= 1 || version <= 3)
		{
			totalSize = KEY_LENGTH_V1_TO_V3;
			zCharLength = KEY_ZCHAR_LENGTH_V1_TO_V3;
		} else
		{
			totalSize = KEY_LENGTH_V4_TO_V6;
			zCharLength = KEY_ZCHAR_LENGTH_V4_TO_V6;
		}
		for(AssembledDictionaryDataElement d : this.elements)
		{
			totalSize += d.getSize();
			if(totalSize > 255)
			{
				// Done inside the loop so it doubles as an overflow check (element size must be <= 255)
				defaultError("Total dictionary entry size may not exceed 255");
			}
		}

		List<Byte> zChars = ZAssemblerUtils.toZChars(key, version);
		if(zChars.size() < zCharLength)
		{
			zChars = new ArrayList<>(zChars);
			zChars.add(Byte.valueOf((byte) 5));
			zChars = Collections.unmodifiableList(zChars);
		} else if(zChars.size() > zCharLength)
		{
			DiagnosticHandler.defaultError("Dictionary entry key is too long for version %d. Expected %d characters, got %d".formatted(version, zCharLength, zChars.size()));
		}
		this.keyZChars = zChars;
	}

	public int getTotalSize()
	{
		return totalSize;
	}

	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		elements.forEach(e -> e.updateResolvedValues(valueReferenceResolver));
	}

	public List<Byte> getKeyBytes()
	{
		return keyZChars;
	}

	public void appendPadded(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler, int size)
	{
		if(size < totalSize)
		{
			diagnosticHandler.error("Requested padded dictionary entry size is less than actual entry size. This is an assembler bug");
			for(int i = 0; i < size; i ++)
			{
				memSeq.writeNextByte(0);
			}
		} else
		{
			ZAssemblerUtils.appendZChars(memSeq, keyZChars);
			elements.forEach(e -> e.append(locationEmitter, memSeq, diagnosticHandler));
			for(int i = totalSize; i < size; i ++)
			{
				memSeq.writeNextByte(0);
			}
		}
	}
}
