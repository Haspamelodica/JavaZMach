package net.haspamelodica.javazmach.assembler.model.entries;

import net.haspamelodica.javazmach.assembler.model.values.HeaderValue;

public record HeaderEntry(String name, HeaderValue value) implements ZAssemblerFileEntry
{}
