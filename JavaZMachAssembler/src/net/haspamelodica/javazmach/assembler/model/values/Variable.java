package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface Variable extends ResolvedOperand, StoreTarget, ResolvedMacroArgument permits StackPointer, LocalVariable, GlobalVariable
{}
