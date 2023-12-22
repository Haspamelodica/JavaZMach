package net.haspamelodica.javazmach.assembler.model;

import java.util.Optional;

public record BranchInfo(boolean branchOnConditionFalse, BranchTarget target, Optional<BranchLength> branchLengthOverride)
{}
