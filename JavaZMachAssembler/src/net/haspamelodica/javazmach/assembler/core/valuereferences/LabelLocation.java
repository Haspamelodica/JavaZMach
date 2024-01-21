package net.haspamelodica.javazmach.assembler.core.valuereferences;

public record LabelLocation(int macroRefId, String label) implements SpecialLocation
{}
