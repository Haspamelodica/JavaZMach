package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

import net.haspamelodica.javazmach.assembler.model.values.BinaryExpression;

public record ResolvedBinaryExpression(ResolvedIntegralValue lhs, BinaryExpression.Op op, ResolvedIntegralValue rhs)
		implements ResolvedIntegralValue
{}
