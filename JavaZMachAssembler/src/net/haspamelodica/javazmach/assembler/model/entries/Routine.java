package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.RoutineLocal;

public record Routine(String name, List<RoutineLocal> locals) implements MacroOrFileEntry
{
	public Routine(String name, List<RoutineLocal> locals)
	{
		this.name = name;
		this.locals = List.copyOf(locals);
	}
}
