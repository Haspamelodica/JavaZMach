package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface IntegralValue extends HeaderValue, IntegralMacroArgument, BranchTarget, Operand
		permits IntegralLiteral, LabelReference, BinaryExpression, UnaryExpression, MacroParamRef
{}
