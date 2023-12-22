package net.haspamelodica.javazmach.assembler.core;

public record CodeLabelRelativeReference(String label, CodeLocation loc) implements ReferenceTarget
{}
