package net.haspamelodica.javazmach.assembler.core;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.BranchLength;

public record BranchTarget(CodeLocation location, Optional<BranchLength> branchLengthOverride) implements ReferenceSource
{}
