package net.haspamelodica.javazmach.core.memory;

import java.util.Arrays;

public class StaticArrayBackedMemory implements ReadOnlyMemory
{
	private final byte[] bytes;

	public StaticArrayBackedMemory(int size)
	{
		this.bytes = new byte[size];
	}
	public StaticArrayBackedMemory(byte[] bytes)
	{
		this.bytes = Arrays.copyOf(bytes, bytes.length);
	}

	@Override
	public int getSize()
	{
		return bytes.length;
	}
	@Override
	public int readByte(int byteAddr)
	{
		return byteAt(byteAddr);
	}
	@Override
	public int readWord(int byteAddr)
	{
		return (byteAt(byteAddr) << 8) | byteAt(byteAddr + 1);
	}
	private int byteAt(int byteAddr)
	{
		return bytes[byteAddr] & 0xFF;
	}
}