package net.haspamelodica.javazmach.assembler.core.assembledentries.objecttable;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;
import static net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext.GLOBAL_MACRO_CONTEXT;

import java.math.BigInteger;
import java.util.List;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.ZAttribute;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledZAttributes
{
	private static record AssembledZAttribute(ResolvableIntegralValue index)
	{}
	private final List<AssembledZAttribute> attributes;

	private final int	version;
	private final int	maxAttribute;

	public AssembledZAttributes(List<ZAttribute> attributes, int version)
	{
		this.attributes = attributes
				.stream()
				.map(ZAttribute::index)
				// object table is always in global context
				.map(GLOBAL_MACRO_CONTEXT::resolve)
				.map(ResolvableIntegralValue::new)
				.map(AssembledZAttribute::new)
				.toList();

		this.version = version;

		if(version >= 1 && version <= 3)
		{
			this.maxAttribute = 32;
		} else if(version >= 4 && version <= 6)
		{
			this.maxAttribute = 48;
		} else
		{
			defaultError("Unknown version: " + version);
			this.maxAttribute = 0;
		}
	}

	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		attributes.forEach(a -> a.index().updateResolvedValue(valueReferenceResolver));
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		byte[] flags = new byte[maxAttribute / 8];

		for(AssembledZAttribute a : attributes)
		{
			BigInteger indexBigint = a.index().resolvedValueOrZero();
			if(indexBigint.signum() < 0 || indexBigint.compareTo(BigInteger.valueOf(maxAttribute)) >= 0)
			{
				defaultError(String.format("Attribute index %d invalid for version %d. Should be in interval [0, %d)", a.index(), version, maxAttribute));
			}
			int index = indexBigint.intValue();
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

		for(byte b : flags)
		{
			memSeq.writeNextByte(b);
		}
	}

}
