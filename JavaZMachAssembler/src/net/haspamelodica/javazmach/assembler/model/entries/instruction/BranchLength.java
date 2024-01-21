package net.haspamelodica.javazmach.assembler.model.entries.instruction;

public enum BranchLength
{
	// don't reorder - AssembledRegularBranchTarget depends on this being sorted in ascending length
	SHORTBRANCH,
	LONGBRANCH;
}
