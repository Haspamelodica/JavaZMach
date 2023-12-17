package net.haspamelodica.javazmach.assembler.model;

public enum RFalse implements BranchTarget
{
	INSTANCE;

	@Override
	public String toString()
	{
		return RFalse.class.getSimpleName();
	}
}
