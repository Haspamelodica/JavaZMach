package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;

import java.math.BigInteger;
import java.util.List;

import net.haspamelodica.javazmach.assembler.model.ZAttribute;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledZAttributes implements AssembledEntry
{
	byte flags[];

	public AssembledZAttributes(List<ZAttribute> attributes, int version)
	{
		int maxAttribute;
		if(version >= 1 && version <= 3)
		{
			maxAttribute = 32;
		} else if(version >= 4 && version <= 6)
		{
			maxAttribute = 48;
		} else
		{
			defaultError("Unknown version: " + version);
			maxAttribute = 0;
		}
		flags = new byte[maxAttribute / 8];
		for(ZAttribute a : attributes)
		{
			if(a.index().signum() < 0 || a.index().compareTo(BigInteger.valueOf(maxAttribute)) >= 0)
			{
				defaultError(String.format("Attribute index %d invalid for version %d. Should be in interval [0, %d)", a.index(), version, maxAttribute));
			}
			int index = a.index().intValue();
			int byteIndex = index / 8;
			// for some reason, the lowest index is in the highest bit (docs section 12.3.1)
			int bitIndex = 7 - index % 8;
			byte bitMask = (byte) (1 << bitIndex);
			if((flags[byteIndex] & bitMask) != 0)
			{
				defaultWarning(String.format("Attribute %d was set multiple times", index));
			} else
			{
				flags[byteIndex] |= bitMask;
			}
		}
	}

	@Override
	public void updateResolvedValues(LocationResolver locationsAndLabels)
	{}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler)
	{
		for(byte b : flags)
		{
			codeSeq.writeNextByte(b);
		}
	}

}
