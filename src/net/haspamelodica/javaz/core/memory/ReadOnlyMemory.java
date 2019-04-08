package net.haspamelodica.javaz.core.memory;

public interface ReadOnlyMemory
{
	public int getSize();
	public int readByte(int byteAddr);
	public int readWord(int byteAddr);
}