package net.haspamelodica.javazmach.assembler.model;

public sealed interface ZAssemblerFileEntry permits HeaderEntry, LabelDeclaration, ZAssemblerInstruction
{}
