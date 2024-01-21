package net.haspamelodica.javazmach.assembler.model.entries.dictionary;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.values.ZString;

public record DictionaryEntry(ZString key, List<DictionaryDataElement> elements)
{}
