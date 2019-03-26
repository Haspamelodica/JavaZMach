package net.haspamelodica.javaz.model.memory;

public interface ReadOnlyMemory
{
	public int readByte(int byteAddr);
	public int readWord(int byteAddr);
}