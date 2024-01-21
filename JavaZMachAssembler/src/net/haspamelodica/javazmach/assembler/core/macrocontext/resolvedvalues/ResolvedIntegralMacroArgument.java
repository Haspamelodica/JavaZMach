package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

public sealed interface ResolvedIntegralMacroArgument extends ResolvedMacroArgument
		permits ResolvedIntegralValue, ResolvedLabelReferenceMacroArgument
{}
