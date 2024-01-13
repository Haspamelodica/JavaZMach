package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record Dictionary(List<CharLiteral> separators, List<DictionaryEntry> entries) implements ZAssemblerFileEntry
{}
