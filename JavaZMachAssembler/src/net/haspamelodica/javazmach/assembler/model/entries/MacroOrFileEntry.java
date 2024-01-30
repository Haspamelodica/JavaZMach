package net.haspamelodica.javazmach.assembler.model.entries;

public sealed interface MacroOrFileEntry extends MacroEntry, ZAssemblerFileEntry
		permits Routine, LabelDeclaration, ZAssemblerInstruction, Buffer, NamedValue, MacroReference, Alignment
{}
