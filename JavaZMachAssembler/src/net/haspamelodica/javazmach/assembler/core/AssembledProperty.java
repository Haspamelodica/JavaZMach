package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.assembler.model.Property;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledProperty
{
	private static final int	MAX_PROP_LENGTH_V1TO3	= 8;
	private static final int	MAX_PROP_LENGTH_V4TO6	= 64;
	private static final int	INDEX_BITS_V1TO3		= 5;
	private static final int	INDEX_BITS_V4TO6		= 6;

	private final Property	property;
	private final int		version;

	public AssembledProperty(Property property, int version)
	{
		this.property = property;
		this.version = version;
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		int indexBits = switch(version)
		{
			case 1, 2, 3 -> INDEX_BITS_V1TO3;
			case 4, 5, 6 -> INDEX_BITS_V4TO6;
			default ->
			{
				diagnosticHandler.error(String.format("Unknown version %d", version));
				yield 0;
			}
		};

		int index = ZAssemblerUtils.bigintIntChecked(indexBits, property.index(), (b) -> String.format("Property index %d is too large for version %d. Should occupy at most %d bits", b, version, indexBits));
		byte propertyBytes[] = ZAssemblerUtils.materializeByteSequence(property.bytes(), (error) -> String.format("Error in property %d: %s", index, error));
		// See section 12.4
		if(version >= 1 && version <= 3)
		{
			if(propertyBytes.length > MAX_PROP_LENGTH_V1TO3)
			{
				diagnosticHandler.error(String.format("Property length %d is too large for version %d. Should be at most %d", propertyBytes.length, version, MAX_PROP_LENGTH_V1TO3));
			}
			int sizeByte = index | (propertyBytes.length - 1) * 32;
			memSeq.writeNextByte(sizeByte);
		} else if(version >= 4 && version <= 6)
		{
			if(propertyBytes.length > MAX_PROP_LENGTH_V4TO6)
			{
				diagnosticHandler.error(String.format("Property length %d is too large for version %d. Should be at most %d", propertyBytes.length, version, MAX_PROP_LENGTH_V4TO6));
			}

			int firstSizeByte = index & 0b00111111;
			if(propertyBytes.length > 2)
			{
				firstSizeByte |= 0b10000000;
				// all bits (except the MSB) will be zero for size 64
				int secondSizeByte = propertyBytes.length & 0b00111111;
				secondSizeByte |= 0b1000000;
				memSeq.writeNextByte(firstSizeByte);
				memSeq.writeNextByte(secondSizeByte);
			} else
			{
				if(propertyBytes.length == 2)
				{
					firstSizeByte |= 0b01000000;
				}
				memSeq.writeNextByte(firstSizeByte);
			}
		}
		for(byte b : propertyBytes)
		{
			memSeq.writeNextByte(b);
		}
	}

}
