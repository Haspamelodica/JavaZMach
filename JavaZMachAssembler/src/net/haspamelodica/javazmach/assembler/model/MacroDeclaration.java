package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record MacroDeclaration(String name, List<MacroParam> params, List<MacroEntry> body) implements ZAssemblerFileEntry
{
	public MacroDeclaration(String name, List<MacroParam> params, List<MacroEntry> body)
	{
		this.name = name;
		this.params = List.copyOf(params);
		this.body = List.copyOf(body);
	}
}
