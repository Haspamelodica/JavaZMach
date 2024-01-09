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

public class AssembledProperties implements AssembledEntry
{
	private static final int MAX_SHORT_NAME_LEN = 765;
	private List<AssembledProperty>	properties;
	private int						objIndex;
	private List<Byte>              nameZChars;
	private int                     nameLength;

	public AssembledProperties(List<Property> properties, ZString name, int objIndex, int version)
	{
		List<Property> sortedProperties = properties.stream().sorted((p1, p2) ->
		{
			// descending order (section 12.4)
			return p2.index().compareTo(p1.index());
		}).toList();
		BigInteger lastProp = null;
		this.properties = new ArrayList<AssembledProperty>();
		// TODO: fix this weird propIndex that I added out of nowhere
		for(Property p : sortedProperties)
		{
			AssembledProperty assembledProp = new AssembledProperty(p, version);
			if(p.index() == lastProp)
			{
				// TODO: print object name
				defaultWarning(String.format("Property with index %s multiply defined. Overwriting...", p.index().toString()));
				this.properties.set(this.properties.size() - 1, assembledProp);
			} else
			{
				this.properties.add(assembledProp);
			}
		}
		this.properties = Collections.unmodifiableList(this.properties);
		this.objIndex = objIndex;
		nameZChars = toZChars(name, version);
		nameLength = zStringWordLength(nameZChars);
		if(nameLength > MAX_SHORT_NAME_LEN) {
			defaultError(String.format("Object short name is restricted to %d zchars, got %d", MAX_SHORT_NAME_LEN, nameLength));
		}
	}

	@Override
	public void updateResolvedValues(LocationResolver locationsAndLabels)
	{
		properties.forEach(p -> p.updateResolvedValues(locationsAndLabels));
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocationHere(new PropertiesLocation(objIndex));
		
		codeSeq.writeNextByte(nameLength);
		ZAssemblerUtils.appendZChars(codeSeq, nameZChars);
		
		for(AssembledProperty property : properties)
		{
			property.append(locationEmitter, codeSeq, diagnosticHandler);
		}
		// Terminate list of properties
		codeSeq.writeNextByte(0);
	}

}
