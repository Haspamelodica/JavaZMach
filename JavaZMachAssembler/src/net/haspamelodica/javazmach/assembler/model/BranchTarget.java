package net.haspamelodica.javazmach.assembler.model;

public sealed interface BranchTarget permits SimpleBranchTarget, ConstantInteger, Label
{}
