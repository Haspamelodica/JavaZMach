package net.haspamelodica.javazmach.assembler.model;

public sealed interface Variable extends ResolvedOperand permits StackPointer, LocalVariable, GlobalVariable
{}
