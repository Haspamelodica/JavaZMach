package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.entries.macro.MacroParamDecl;

public record MacroDeclaration(String name, List<MacroParamDecl> params, List<MacroEntry> body) implements ZAssemblerFileEntry
{
	public MacroDeclaration(String name, List<MacroParamDecl> params, List<MacroEntry> body)
	{
		this.name = name;
		this.params = List.copyOf(params);
		this.body = List.copyOf(body);
	}
}
