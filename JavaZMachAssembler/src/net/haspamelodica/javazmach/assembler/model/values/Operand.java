package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface Operand permits IntegralValue, Variable, MacroParam
{}
