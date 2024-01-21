package net.haspamelodica.javazmach.assembler.model;

public sealed interface ResolvedMacroArgument extends MacroArgument permits IntegralMacroArgument, Variable
{}
