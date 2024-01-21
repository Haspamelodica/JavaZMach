package net.haspamelodica.javazmach.assembler.core.assembledentries;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.haspamelodica.javazmach.assembler.core.DiagnosticHandler;
import net.haspamelodica.javazmach.assembler.core.assembledentries.objecttable.AssembledDefaultProperties;
import net.haspamelodica.javazmach.assembler.core.assembledentries.objecttable.AssembledProperties;
import net.haspamelodica.javazmach.assembler.core.assembledentries.objecttable.AssembledZObject;
import net.haspamelodica.javazmach.assembler.core.valuereferences.SpecialDataStructureLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.SpecialLocationEmitter;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.ZObjectTable;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.Property;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.ZAttribute;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.ZObject;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.ZObjectEntry;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledZObjectTable implements AssembledEntry
{
	private static final int MIN_OBJECT_INDEX = 1;

	private final AssembledDefaultProperties	defaultProperties;
	private final List<AssembledZObject>		objects;
	private final List<AssembledProperties>		properties;

	public AssembledZObjectTable(ZObjectTable table, int version)
	{
		defaultProperties = new AssembledDefaultProperties(table.defaultProperties(), version);
		List<AssembledZObject> objects = new ArrayList<AssembledZObject>();
		properties = new ArrayList<AssembledProperties>();
		linearizeObjects(table.objects(), 0, objects, properties, version);
		this.objects = Collections.unmodifiableList(objects);
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
			boolean hasSibling = ++ i != objects.size();
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
		// object indices are always offset by 1
		int realIndex = objects.size();
		int objectIndex = realIndex + MIN_OBJECT_INDEX;
		objects.add(null);
		properties.add(null);
		int firstChildIndex = children.isEmpty() ? 0 : objectIndex + 1;

		linearizeObjects(children, objectIndex, objects, properties, version);

		int siblingIndex = hasSibling ? objects.size() + MIN_OBJECT_INDEX : 0;
		objects.set(realIndex, new AssembledZObject(root.ident(), localAttributes, objectIndex, parentIndex, siblingIndex, firstChildIndex, version));
		properties.set(realIndex, new AssembledProperties(localProperties, root.name(), objectIndex, version));
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		objects.forEach(o -> o.updateResolvedValues(valueReferenceResolver));
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
