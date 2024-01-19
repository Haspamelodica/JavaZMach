package net.haspamelodica.javazmach.assembler.model;

public sealed interface ZAssemblerFileEntry
		permits HeaderEntry, ZObjectTable, GlobalVarTable, Dictionary, SectionDeclaration, Macro, MacroOrFileEntry
{}
