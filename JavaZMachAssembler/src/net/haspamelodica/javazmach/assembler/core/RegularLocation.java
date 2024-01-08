package net.haspamelodica.javazmach.assembler.core;

public record RegularLocation(AssembledEntry entry, AssembledEntryPart targetPart) implements Location
{
	public static enum AssembledEntryPart
	{
		START,
		// BRANCH_ORIGIN is handled by the special BranchOriginLocation
		AFTER;
	}
}
