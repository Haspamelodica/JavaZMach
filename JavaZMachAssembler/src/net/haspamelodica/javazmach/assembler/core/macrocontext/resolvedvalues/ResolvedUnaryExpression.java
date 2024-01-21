package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

import net.haspamelodica.javazmach.assembler.model.values.UnaryExpression;

public record ResolvedUnaryExpression(UnaryExpression.Op op, ResolvedIntegralValue operand) implements ResolvedIntegralValue
{}
