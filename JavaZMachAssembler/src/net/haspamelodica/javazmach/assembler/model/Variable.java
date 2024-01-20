package net.haspamelodica.javazmach.assembler.model;

public sealed interface Variable extends ResolvedOperand, StoreTarget permits StackPointer, LocalVariable, GlobalVariable
{}
