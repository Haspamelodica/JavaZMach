package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record Routine(String name, List<RoutineLocal> locals) implements ZAssemblerFileEntry
{
	public Routine(String name, List<RoutineLocal> locals)
	{
		this.name = name;
		this.locals = List.copyOf(locals);
	}
}
