package net.haspamelodica.javazmach.assembler.core;

public sealed interface SpecialLocation extends ValueReference
		permits BranchOriginLocation, LabelLocation, PropertiesLocation, SpecialDataStructureLocation, ExplicitSectionLocation
{}
