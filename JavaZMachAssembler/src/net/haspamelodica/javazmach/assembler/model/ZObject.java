package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record ZObject(ZString name, List<ZObjectEntry> entries) implements ZObjectEntry
{
	public ZObject(ZString name, List<ZObjectEntry> entries)
	{
		this.name = name;
		this.entries = List.copyOf(entries);
	}
}
