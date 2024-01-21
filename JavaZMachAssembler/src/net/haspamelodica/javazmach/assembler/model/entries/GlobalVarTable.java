package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.entries.globaltable.Global;

public record GlobalVarTable(List<Global> globals) implements ZAssemblerFileEntry
{}
