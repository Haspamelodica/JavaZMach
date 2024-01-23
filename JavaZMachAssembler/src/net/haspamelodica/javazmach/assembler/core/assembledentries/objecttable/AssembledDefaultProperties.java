package net.haspamelodica.javazmach.assembler.core.assembledentries.objecttable;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultHandler;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.materializeByteSequence;
import static net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext.GLOBAL_MACRO_CONTEXT;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ResolvableIntegralValue;
import net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.Property;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledDefaultProperties
{
	// property indices start at 1 (for some reason)
	private final static int MIN_PROPERTY = 1;

	private static record AssembledDefaultProperty(ResolvableIntegralValue index, byte[] value)
	{}
	private final List<AssembledDefaultProperty> defaultProperties;

	private final int	version;
	private final int	defaultPropertyCount;

	public AssembledDefaultProperties(List<Property> defaultProperties, int version)
	{
		this.defaultProperties = defaultProperties.stream().map(p ->
		{
			byte[] value = materializeByteSequence(p.bytes(), version, (error) -> "Error in default property: ");
			if(value.length > 2)
				defaultError(String.format("Default property values must have at most 2 bytes. Got %d bytes", value.length));
			else if(value.length < 2)
				value = Arrays.copyOf(value, 2);
			// object table is always in global context
			return new AssembledDefaultProperty(new ResolvableIntegralValue(GLOBAL_MACRO_CONTEXT.resolve(p.index())), value);
		}).toList();

		this.version = version;

		if(version >= 1 && version <= 3)
		{
			this.defaultPropertyCount = 31;
		} else if(version >= 4 && version <= 6)
		{
			this.defaultPropertyCount = 63;
		} else
		{
			this.defaultPropertyCount = 0;
			defaultError("Unknown version " + version);
		}
	}

	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		defaultProperties.forEach(a -> a.index().updateResolvedValue(valueReferenceResolver));
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		List<Optional<byte[]>> assembledDefaultProperties = new ArrayList<>(defaultPropertyCount);
		for(int i = 0; i < defaultPropertyCount; i ++)
		{
			assembledDefaultProperties.add(Optional.empty());
		}

		for(AssembledDefaultProperty p : defaultProperties)
		{
			BigInteger bigIndex = p.index().resolvedValueOrZero();
			// Not a very precise error message, but
			// that index is unreasonably large.
			final int index = ZAssemblerUtils.bigintIntChecked(32, bigIndex, (b) -> "Property index " + b + " too large", defaultHandler());
			if(index < MIN_PROPERTY || index >= MIN_PROPERTY + defaultPropertyCount)
				defaultError(String.format("Unexpected property index: %d. Property indices must be in interval [%d, %d) in version %d",
						index, MIN_PROPERTY, MIN_PROPERTY + defaultPropertyCount, version));
			else
			{
				if(assembledDefaultProperties.get(index - MIN_PROPERTY).isPresent())
					defaultWarning(String.format("Default property %d is multiply defined. Overwriting it...", index));
				assembledDefaultProperties.set(index - MIN_PROPERTY, Optional.of(p.value()));
			}
		}

		for(Optional<byte[]> p : assembledDefaultProperties)
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
