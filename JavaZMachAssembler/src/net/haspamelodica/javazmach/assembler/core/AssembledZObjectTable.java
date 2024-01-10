package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.haspamelodica.javazmach.assembler.model.Property;
import net.haspamelodica.javazmach.assembler.model.ZAttribute;
import net.haspamelodica.javazmach.assembler.model.ZObject;
import net.haspamelodica.javazmach.assembler.model.ZObjectEntry;
import net.haspamelodica.javazmach.assembler.model.ZObjectTable;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledZObjectTable implements AssembledEntry
{
	AssembledDefaultProperties	defaultProperties;
	List<AssembledZObject>		objects;
	List<AssembledProperties>	properties;

	public AssembledZObjectTable(ZObjectTable table, int version)
	{
		defaultProperties = new AssembledDefaultProperties(table.defaultProperties(), version);
		objects = new ArrayList<AssembledZObject>();
		properties = new ArrayList<AssembledProperties>();
		linearizeObjects(table.objects(), 0, objects, properties, version);
		objects = Collections.unmodifiableList(objects);
		if(version >= 1 && version <= 3)
		{
			if(objects.size() > 255)
				defaultError(String.format("Too many objects for version %d. Maximum allowed is %d, found %d", version, 255, objects.size()));
		} else if(version >= 4 && version <= 6)
		{
			// I'd like to see that...
			if(objects.size() > 65535)
				defaultError(String.format("Too many objects for version %d. Maximum allowed is %d, found %d", version, 65535, objects.size()));
		} else
		{
			defaultError(String.format("Unknown version %d", version));
		}
	}

	private void linearizeObjects(List<ZObject> objects, int parentIndex, List<AssembledZObject> assembledObjects, List<AssembledProperties> assembledProperties, int version)
	{
		int i = 0;
		for(ZObject o : objects)
		{
			boolean hasSibling = ++ i == objects.size();
			linearizeObjects(o, 0, hasSibling, assembledObjects, assembledProperties, version);
		}
	}

	private void linearizeObjects(ZObject root, int parentIndex, boolean hasSibling, List<AssembledZObject> objects, List<AssembledProperties> properties, int version)
	{
		List<Property> localProperties = new ArrayList<Property>();
		List<ZObject> children = new ArrayList<ZObject>();
		List<ZAttribute> localAttributes = new ArrayList<ZAttribute>();

		for(ZObjectEntry entry : root.entries())
		{
			switch(entry)
			{
				case Property prop -> localProperties.add(prop);
				case ZObject child -> children.add(child);
				case ZAttribute attribute -> localAttributes.add(attribute);
			}
		}
		int index = objects.size();
		objects.add(null);
		properties.add(null);
		int firstChildIndex = children.isEmpty() ? 0 : index + 1;

		linearizeObjects(children, index, objects, properties, version);

		int siblingIndex = hasSibling ? 0 : objects.size();
		objects.set(index, new AssembledZObject(localAttributes, index, parentIndex, firstChildIndex, siblingIndex, version));
		properties.set(index, new AssembledProperties(localProperties, root.name(), index, version));
	}

	@Override
	public void updateResolvedValues(LocationResolver locationResolver)
	{
		defaultProperties.updateResolvedValues(locationResolver);
		objects.forEach(o -> o.updateResolvedValues(locationResolver));
		properties.forEach(o -> o.updateResolvedValues(locationResolver));
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocationHere(SpecialDataStructureLocation.OBJ_TABLE);
		defaultProperties.append(locationEmitter, memSeq, diagnosticHandler);
		// Seemingly, it is not required to mark the end of the object table
		objects.forEach(o -> o.append(locationEmitter, memSeq, diagnosticHandler));
		properties.forEach(p -> p.append(locationEmitter, memSeq, diagnosticHandler));
	}

}
