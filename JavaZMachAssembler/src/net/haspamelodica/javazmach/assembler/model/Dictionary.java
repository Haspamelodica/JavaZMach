package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record Dictionary(List<DictionaryEntry> entries) implements ZAssemblerFileEntry
{}
