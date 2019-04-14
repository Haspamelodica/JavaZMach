package net.haspamelodica.javazmach.core.memory;

public interface WritableMemory extends ReadOnlyMemory
{
	public void writeByte(int byteAddr, int val);
	public void writeWord(int byteAddr, int val);
}