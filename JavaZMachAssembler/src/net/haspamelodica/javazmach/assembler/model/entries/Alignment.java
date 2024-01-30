package net.haspamelodica.javazmach.assembler.model.entries;

import net.haspamelodica.javazmach.assembler.model.values.AlignmentValue;

public record Alignment(AlignmentValue alignment) implements MacroOrFileEntry
{}
