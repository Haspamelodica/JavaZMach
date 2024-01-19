package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record MacroReference(String name, List<Operand> args) implements MacroOrFileEntry
{}
