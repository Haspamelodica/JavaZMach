package net.haspamelodica.javaz;

public interface ReadOnlyMemory
{
	public int readByte(int byteAddr);
	public int readWord(int byteAddr);
}