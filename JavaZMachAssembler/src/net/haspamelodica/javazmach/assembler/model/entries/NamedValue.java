package net.haspamelodica.javazmach.assembler.model.entries;

import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public record NamedValue(IdentifierDeclaration ident, IntegralValue value) implements MacroOrFileEntry
{}
