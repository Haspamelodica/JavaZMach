package net.haspamelodica.javazmach.assembler.model;

import java.util.List;
import java.util.OptionalInt;

public record ZAssemblerFile(OptionalInt version, List<ZAssemblerFileEntry> entries)
{
	public ZAssemblerFile(OptionalInt version, List<ZAssemblerFileEntry> entries)
	{
		this.version = version;
		this.entries = List.copyOf(entries);
	}
}
