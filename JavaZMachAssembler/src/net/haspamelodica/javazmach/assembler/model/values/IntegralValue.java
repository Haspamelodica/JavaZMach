package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface IntegralValue extends HeaderValue, MacroArgument, BranchTarget, Operand
		permits IntegralLiteral, LabelReferenceIntegralOnly, BinaryExpression, UnaryExpression, MacroParamRef
{}
