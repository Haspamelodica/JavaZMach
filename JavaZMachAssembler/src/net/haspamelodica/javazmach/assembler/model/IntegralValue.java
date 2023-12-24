package net.haspamelodica.javazmach.assembler.model;

public sealed interface IntegralValue extends HeaderValue, BranchTarget, Operand
		permits IntegralLiteral, LabelReference, BinaryExpression, UnaryExpression
{}
