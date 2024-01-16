package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.materializeByteSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.Property;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledDefaultProperties
{
	// property indices start at 1 (for some reason)
	private final static int MIN_PROPERTY = 1;

	private final List<Optional<byte[]>> defaultProperties;

	public AssembledDefaultProperties(List<Property> defaultProperties, int version)
	{
		int defaultPropertyCount;
		if(version >= 1 && version <= 3)
		{
			defaultPropertyCount = 31;
		} else if(version >= 4 && version <= 6)
		{
			defaultPropertyCount = 63;
		} else
		{
			defaultPropertyCount = 0;
			defaultError("Unknown version " + version);
		}

		this.defaultProperties = new ArrayList<Optional<byte[]>>(defaultPropertyCount);
		for(int i = 0; i < defaultPropertyCount; i ++)
		{
			this.defaultProperties.add(Optional.empty());
		}

		for(Property p : defaultProperties)
		{
			// Not a very precise error message, but
			// that index is unreasonably large.
			final int index = ZAssemblerUtils.bigintIntChecked(32, p.index(), (bigIndex) -> "Property index " + bigIndex + " too large", DiagnosticHandler.defaultHandler());
			if(index >= MIN_PROPERTY && index < MIN_PROPERTY + defaultPropertyCount)
			{
				byte[] value = materializeByteSequence(p.bytes(), version,(error) -> "Error in default property " + index + ": ");
				if(value.length > 2)
				{
					defaultError(String.format("Default property %d values must have at most 2 bytes. Got %d bytes", index, value.length));
				}
				if(this.defaultProperties.get(index - MIN_PROPERTY).isPresent())
				{
					defaultWarning(String.format("Default property %d is multiply defined. Overwriting it...", index));
				}
				this.defaultProperties.set(index - MIN_PROPERTY, Optional.of(value));

			} else
			{
				defaultError(String.format("Unexpected property index: %d. Property indices must be in interval [%d, %d) in version %d", index, MIN_PROPERTY, MIN_PROPERTY + defaultPropertyCount, version));
			}
		}
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		for(Optional<byte[]> p : defaultProperties)
		{
			p.ifPresentOrElse((bytes) ->
			{
				for(byte b : bytes)
				{
					memSeq.writeNextByte(b);
				}
			}, () ->
			{
				memSeq.writeNextWord(0);
			});
		}
	}

}
