package net.haspamelodica.javazmach.assembler.core;

public sealed interface Location permits SimpleLocation, Section, CodeLocation, LabelLocation
{}
