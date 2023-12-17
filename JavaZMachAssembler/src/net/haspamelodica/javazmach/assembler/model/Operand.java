package net.haspamelodica.javazmach.assembler.model;

public sealed interface Operand permits Constant, Variable, Label
{}
