package net.haspamelodica.javazmach.assembler.model;

public sealed interface IntegralValue extends HeaderValue, BranchTarget, ResolvedOperand
		permits IntegralLiteral, LabelReference, BinaryExpression, UnaryExpression, MacroParamRef
{}
