package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.entries.dictionary.DictionaryEntry;
import net.haspamelodica.javazmach.assembler.model.values.CharLiteral;

public record Dictionary(List<CharLiteral> separators, List<DictionaryEntry> entries) implements ZAssemblerFileEntry
{}
