package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface IntegralMacroArgument extends ResolvedMacroArgument permits IntegralValue, LabelReferenceMacroArgument
{}
