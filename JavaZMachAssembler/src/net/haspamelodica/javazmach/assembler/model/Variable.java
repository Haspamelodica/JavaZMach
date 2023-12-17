package net.haspamelodica.javazmach.assembler.model;

public sealed interface Variable extends Operand permits StackPointer, LocalVariable, GlobalVariable
{}
