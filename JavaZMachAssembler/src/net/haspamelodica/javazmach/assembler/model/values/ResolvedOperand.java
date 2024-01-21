package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface ResolvedOperand extends Operand permits IntegralValue, Variable
{}
