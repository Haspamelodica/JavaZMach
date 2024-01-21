package net.haspamelodica.javazmach.assembler.core.valuereferences;

public sealed interface SpecialLocation extends ValueReference
		permits BranchOriginLocation, LabelLocation, PropertiesLocation, SpecialDataStructureLocation, ExplicitSectionLocation
{}
