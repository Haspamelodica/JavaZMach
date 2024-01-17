package net.haspamelodica.javazmach.assembler.model;

import java.util.List;
import java.util.Optional;

public record ZObject(Optional<String> ident, ZString name, List<ZObjectEntry> entries) implements ZObjectEntry
{
	public ZObject(Optional<String> ident, ZString name, List<ZObjectEntry> entries)
	{
		this.name = name;
		this.ident = ident;
		this.entries = List.copyOf(entries);
	}
}
