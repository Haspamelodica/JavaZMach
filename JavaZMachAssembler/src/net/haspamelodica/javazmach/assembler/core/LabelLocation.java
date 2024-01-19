package net.haspamelodica.javazmach.assembler.core;

public record LabelLocation(int macroRefId, String label) implements SpecialLocation
{}
