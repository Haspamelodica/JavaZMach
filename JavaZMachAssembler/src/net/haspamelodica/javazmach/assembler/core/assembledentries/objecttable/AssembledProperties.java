package net.haspamelodica.javazmach.assembler.core.assembledentries.objecttable;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.toZChars;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.zStringWordLength;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils;
import net.haspamelodica.javazmach.assembler.core.valuereferences.PropertiesLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.Property;
import net.haspamelodica.javazmach.assembler.model.values.ZString;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledProperties
{
	private static final int MAX_SHORT_NAME_LEN = 765;

	private final List<AssembledProperty>	properties;
	private final int						objIndex;
	private final List<Byte>				nameZChars;
	private final int						nameLength;

	public AssembledProperties(List<Property> properties, ZString name, int objIndex, int version)
	{
		this.properties = properties.stream().map(p -> new AssembledProperty(p, version)).toList();
		this.objIndex = objIndex;
		nameZChars = toZChars(name, version);
		nameLength = zStringWordLength(nameZChars);
		if(nameLength > MAX_SHORT_NAME_LEN)
		{
			defaultError(String.format("Object short name is restricted to %d zchars, got %d", MAX_SHORT_NAME_LEN, nameLength));
		}
	}

	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		properties.forEach(p -> p.updateResolvedValues(valueReferenceResolver));
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocationHere(new PropertiesLocation(objIndex));

		memSeq.writeNextByte(nameLength);
		ZAssemblerUtils.appendZChars(memSeq, nameZChars);


		// descending order (section 12.4)
		Stream<AssembledProperty> sortedProperties = properties.stream().sorted(Comparator.comparing(AssembledProperty::resolvedIndexOrZero));
		BigInteger lastIndex = null;
		for(AssembledProperty property : (Iterable<AssembledProperty>) sortedProperties::iterator)
		{
			BigInteger index = property.resolvedIndexOrZero();
			if(lastIndex == null || !lastIndex.equals(index))
				property.append(locationEmitter, memSeq, diagnosticHandler);
			else
				diagnosticHandler.warning("Property with index %d defined multiple times - keeping first value".formatted(index));
			lastIndex = index;
		}
		// Terminate list of properties
		memSeq.writeNextByte(0);
	}
}
