package net.haspamelodica.javazmach.assembler.model;

public sealed interface IntegralMacroArgument extends ResolvedMacroArgument permits IntegralValue, LabelReferenceMacroArgument
{}
