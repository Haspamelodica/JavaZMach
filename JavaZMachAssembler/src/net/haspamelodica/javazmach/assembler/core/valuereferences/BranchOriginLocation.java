package net.haspamelodica.javazmach.assembler.core.valuereferences;

import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledInstruction;

public record BranchOriginLocation(AssembledInstruction instruction) implements SpecialLocation
{}
