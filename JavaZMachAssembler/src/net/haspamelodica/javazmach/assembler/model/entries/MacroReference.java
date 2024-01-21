package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.values.MacroArgument;

public record MacroReference(String name, List<MacroArgument> args) implements MacroOrFileEntry
{}
