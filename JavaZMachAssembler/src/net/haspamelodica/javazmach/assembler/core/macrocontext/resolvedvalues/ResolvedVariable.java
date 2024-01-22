package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

public sealed interface ResolvedVariable extends ResolvedMacroArgument, ResolvedOperand
		permits ResolvedVariableConstant, ResolvedLabelReferenceVariableOnly
{}
