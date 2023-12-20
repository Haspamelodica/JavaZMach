package net.haspamelodica.javazmach.assembler.model;

public sealed interface BranchTarget permits RFalse, RTrue, ConstantInteger, Label
{}
