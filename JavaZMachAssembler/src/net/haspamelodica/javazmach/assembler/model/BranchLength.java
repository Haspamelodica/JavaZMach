package net.haspamelodica.javazmach.assembler.model;

public enum BranchLength
{
	// don't reorder - AssembledRegularBranchTarget depends on this being sorted in ascending length
	SHORTBRANCH,
	LONGBRANCH;
}
