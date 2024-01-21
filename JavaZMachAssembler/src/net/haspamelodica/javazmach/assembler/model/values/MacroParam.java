package net.haspamelodica.javazmach.assembler.model.values;

public record MacroParam(String name) implements Operand, StoreTarget, MacroArgument
{}
