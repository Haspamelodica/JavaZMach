package net.haspamelodica.javazmach.assembler.core.assembledentries;

import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;
import net.haspamelodica.javazmach.assembler.core.valuereferences.LabelLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.SpecialLocation;

public record AssembledIdentifierDeclaration(MacroContext macroContext, String name)
{
	public SpecialLocation asLabelLocation()
	{
		return new LabelLocation(macroContext().refId(), name());
	}
}
