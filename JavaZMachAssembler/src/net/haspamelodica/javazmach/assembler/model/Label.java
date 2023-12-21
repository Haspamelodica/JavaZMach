package net.haspamelodica.javazmach.assembler.model;

public record Label(String name) implements BranchTarget, HeaderValue
{}
