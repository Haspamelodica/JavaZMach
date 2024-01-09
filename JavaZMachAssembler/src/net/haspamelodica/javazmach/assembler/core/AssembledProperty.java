package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.assembler.model.Property;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledProperty implements AssembledEntry
{
	private static final int	MAX_PROP_LENGTH_V1TO3	= 8;
	private static final int	MAX_PROP_LENGTH_V4TO6	= 64;
	private Property			property;
	private int					index, version;

	public AssembledProperty(Property property, int index, int version)
	{
		this.property = property;
		this.index = index;
		this.version = version;
	}

	@Override
	public void updateResolvedValues(LocationResolver locationsAndLabels)
	{}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler)
	{
		byte propertyBytes[] = ZAssemblerUtils.materializeByteSequence(property.bytes(), (error) -> String.format("Error in property %d: %s", index, error));
		// See section 12.4
		if(version >= 1 && version <= 3)
		{
			if(propertyBytes.length > MAX_PROP_LENGTH_V1TO3)
			{
				diagnosticHandler.error(String.format("Property length %d is too large for version %d. Should be at most %d", propertyBytes.length, version, MAX_PROP_LENGTH_V1TO3));
			}
			int sizeByte = index | propertyBytes.length * 32 - 1;
			codeSeq.writeNextByte(sizeByte);
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
				codeSeq.writeNextByte(firstSizeByte);
				codeSeq.writeNextByte(secondSizeByte);
			} else
			{
				if(propertyBytes.length == 2)
				{
					firstSizeByte |= 0b01000000;
				}
				codeSeq.writeNextByte(firstSizeByte);
			}
			int sizeByte = index | propertyBytes.length * 32 - 1;
			codeSeq.writeNextByte(sizeByte);
		}
		for(byte b : propertyBytes)
		{
			codeSeq.writeNextByte(b);
		}
	}

}
