package net.haspamelodica.javazmach.assembler.model;

public enum RTrue implements BranchTarget
{
	INSTANCE;

	@Override
	public String toString()
	{
		return RTrue.class.getSimpleName();
	}
}
