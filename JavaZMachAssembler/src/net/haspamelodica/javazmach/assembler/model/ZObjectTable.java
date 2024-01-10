package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record ZObjectTable(List<Property> defaultProperties, List<ZObject> objects) implements ZAssemblerFileEntry
{
	public ZObjectTable(List<Property> defaultProperties, List<ZObject> objects)
	{
		this.defaultProperties = List.copyOf(defaultProperties);
		this.objects = List.copyOf(objects);
	}
}
