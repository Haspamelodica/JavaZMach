package net.haspamelodica.javazmach.assembler.model;

public record MacroParam(String name) implements Operand, StoreTarget, MacroArgument
{}
