package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.entries.objecttable.Property;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.ZObject;

public record ZObjectTable(List<Property> defaultProperties, List<ZObject> objects) implements ZAssemblerFileEntry
{
	public ZObjectTable(List<Property> defaultProperties, List<ZObject> objects)
	{
		this.defaultProperties = List.copyOf(defaultProperties);
		this.objects = List.copyOf(objects);
	}
}
