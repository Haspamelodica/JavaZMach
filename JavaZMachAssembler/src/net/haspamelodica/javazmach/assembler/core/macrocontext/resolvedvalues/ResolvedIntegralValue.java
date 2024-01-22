package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

public sealed interface ResolvedIntegralValue extends ResolvedOperand, ResolvedMacroArgument
		permits ResolvedIntegralLiteral, ResolvedLabelReferenceIntegralOnly, ResolvedBinaryExpression, ResolvedUnaryExpression
{}
