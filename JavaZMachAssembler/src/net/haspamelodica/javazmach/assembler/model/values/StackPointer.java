package net.haspamelodica.javazmach.assembler.model.values;

public enum StackPointer implements Variable
{
	INSTANCE;

	@Override
	public String toString()
	{
		return StackPointer.class.getSimpleName();
	}
}
