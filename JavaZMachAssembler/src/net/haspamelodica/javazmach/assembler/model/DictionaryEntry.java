package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record DictionaryEntry(ZString key, List<DictionaryDataElement> elements)
{}
