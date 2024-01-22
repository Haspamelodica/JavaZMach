package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.entries.routine.RoutineLocal;

public record Routine(IdentifierDeclaration ident, List<RoutineLocal> locals) implements MacroOrFileEntry
{
	public Routine(IdentifierDeclaration ident, List<RoutineLocal> locals)
	{
		this.ident = ident;
		this.locals = List.copyOf(locals);
	}
}
