package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface ResolvedMacroArgument extends MacroArgument permits IntegralMacroArgument, Variable
{}
