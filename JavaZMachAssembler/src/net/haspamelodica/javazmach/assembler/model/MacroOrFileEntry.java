package net.haspamelodica.javazmach.assembler.model;

public sealed interface MacroOrFileEntry extends MacroEntry, ZAssemblerFileEntry
		permits Routine, LabelDeclaration, ZAssemblerInstruction, Buffer, NamedValue, MacroReference
{}
