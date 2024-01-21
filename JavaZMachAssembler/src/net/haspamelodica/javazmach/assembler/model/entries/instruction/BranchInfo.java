package net.haspamelodica.javazmach.assembler.model.entries.instruction;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.values.BranchTarget;

public record BranchInfo(boolean branchOnConditionFalse, BranchTarget target, Optional<BranchLength> branchLengthOverride)
{}
