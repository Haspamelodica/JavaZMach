package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record MacroReference(String name, List<MacroArgument> args) implements MacroOrFileEntry
{}
