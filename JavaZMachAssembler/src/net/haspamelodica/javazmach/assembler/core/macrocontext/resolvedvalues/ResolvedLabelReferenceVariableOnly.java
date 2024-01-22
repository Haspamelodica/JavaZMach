package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;

public record ResolvedLabelReferenceVariableOnly(MacroContext macroContext, String name) implements ResolvedVariable
{}
