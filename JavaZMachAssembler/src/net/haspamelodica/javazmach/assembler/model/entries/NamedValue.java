package net.haspamelodica.javazmach.assembler.model.entries;

import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public record NamedValue(String name, IntegralValue value) implements MacroOrFileEntry
{}
