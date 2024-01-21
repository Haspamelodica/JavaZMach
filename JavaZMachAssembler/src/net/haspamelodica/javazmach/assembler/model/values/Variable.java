package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface Variable extends Operand, StoreTarget, MacroArgument permits StackPointer, LocalVariable, GlobalVariable
{}
