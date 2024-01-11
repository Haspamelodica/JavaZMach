package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.assembler.model.Section;

public record ExplicitSectionLocation(Section section) implements SpecialLocation
{}
