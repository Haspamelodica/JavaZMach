package net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues;

import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;

public record ResolvedLabelReferenceIntegralOnly(MacroContext macroContext, String name) implements  ResolvedIntegralValue
{}
