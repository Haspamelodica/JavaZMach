package net.haspamelodica.javazmach.assembler.model.entries.objecttable;

import java.util.List;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.values.ZString;

public record ZObject(Optional<String> ident, ZString name, List<ZObjectEntry> entries) implements ZObjectEntry
{
	public ZObject(Optional<String> ident, ZString name, List<ZObjectEntry> entries)
	{
		this.name = name;
		this.ident = ident;
		this.entries = List.copyOf(entries);
	}
}
