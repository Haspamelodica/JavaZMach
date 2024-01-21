package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

import net.haspamelodica.javazmach.assembler.model.values.Variable;

public record ResolvedVariable(Variable variable) implements ResolvedMacroArgument, ResolvedOperand
{}
