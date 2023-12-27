package net.haspamelodica.javazmach.assembler.core;

public record CodeLocation(AssembledInstruction instruction, InstructionPart targetPart) implements Location
{
	public static enum InstructionPart
	{
		BRANCH_ORIGIN,
		AFTER;
	}
}
