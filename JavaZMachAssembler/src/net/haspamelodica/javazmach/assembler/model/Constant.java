package net.haspamelodica.javazmach.assembler.model;

public record Constant(int value) implements Operand, HeaderValue, BranchTarget
{}
