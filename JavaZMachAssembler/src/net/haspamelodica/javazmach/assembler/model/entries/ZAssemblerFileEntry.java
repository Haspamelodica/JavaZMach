package net.haspamelodica.javazmach.assembler.model.entries;

public sealed interface ZAssemblerFileEntry
		permits HeaderEntry, ZObjectTable, GlobalVarTable, Dictionary, SectionDeclaration, MacroDeclaration, MacroOrFileEntry
{}
