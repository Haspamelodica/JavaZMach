package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

public sealed interface ResolvedIntegralValue extends ResolvedIntegralMacroArgument, ResolvedOperand
		permits ResolvedIntegralLiteral, ResolvedLabelReference, ResolvedBinaryExpression, ResolvedUnaryExpression
{}
