package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.toZChars;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.zStringWordLength;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.haspamelodica.javazmach.assembler.model.Property;
import net.haspamelodica.javazmach.assembler.model.ZString;
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
		List<Property> sortedProperties = properties.stream().sorted((p1, p2) ->
		{
			// descending order (section 12.4)
			return p2.index().compareTo(p1.index());
		}).toList();
		BigInteger lastProp = null;
		List<AssembledProperty> assembledProperties = new ArrayList<AssembledProperty>();
		// TODO: fix this weird propIndex that I added out of nowhere
		for(Property p : sortedProperties)
		{
			AssembledProperty assembledProp = new AssembledProperty(p, version);
			if(p.index() == lastProp)
			{
				// TODO: print object name
				defaultWarning(String.format("Property with index %s multiply defined. Overwriting...", p.index().toString()));
				assembledProperties.set(assembledProperties.size() - 1, assembledProp);
			} else
			{
				assembledProperties.add(assembledProp);
			}
		}
		this.properties = Collections.unmodifiableList(assembledProperties);
		this.objIndex = objIndex;
		nameZChars = toZChars(name, version);
		nameLength = zStringWordLength(nameZChars);
		if(nameLength > MAX_SHORT_NAME_LEN)
		{
			defaultError(String.format("Object short name is restricted to %d zchars, got %d", MAX_SHORT_NAME_LEN, nameLength));
		}
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocationHere(new PropertiesLocation(objIndex));

		memSeq.writeNextByte(nameLength);
		ZAssemblerUtils.appendZChars(memSeq, nameZChars);

		for(AssembledProperty property : properties)
		{
			property.append(locationEmitter, memSeq, diagnosticHandler);
		}
		// Terminate list of properties
		memSeq.writeNextByte(0);
	}

}
