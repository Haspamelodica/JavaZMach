package net.haspamelodica.javazmach.assembler.model;

public sealed interface ResolvedOperand extends Operand permits IntegralValue, Variable
{}
