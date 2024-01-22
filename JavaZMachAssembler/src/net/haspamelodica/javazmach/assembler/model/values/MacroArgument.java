package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface MacroArgument permits IntegralValue, Variable, LabelReferenceMacroArgument, MacroParam
{}
