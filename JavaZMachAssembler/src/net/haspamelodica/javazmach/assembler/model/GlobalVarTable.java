package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record GlobalVarTable(List<Global> globals) implements ZAssemblerFileEntry
{}
