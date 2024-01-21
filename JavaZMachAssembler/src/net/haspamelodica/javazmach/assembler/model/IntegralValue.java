package net.haspamelodica.javazmach.assembler.model;

public sealed interface IntegralValue extends HeaderValue, IntegralMacroArgument, BranchTarget, ResolvedOperand
		permits IntegralLiteral, LabelReference, BinaryExpression, UnaryExpression, MacroParamRef
{}
