package net.haspamelodica.javaz;

import java.util.Arrays;

public class Memory
{
	private final byte[] bytes;

	public Memory(int size)
	{
		this.bytes = new byte[size];
	}
	public Memory(byte[] bytes)
	{
		this.bytes = Arrays.copyOf(bytes, bytes.length);
	}

	public int getSize()
	{
		return bytes.length;
	}
	public int readByte(int byteAddr)
	{
		return byteAt(byteAddr);
	}
	public int readWord(int byteAddr)
	{
		return (byteAt(byteAddr) << 8) | byteAt(byteAddr + 1);
	}
	private int byteAt(int byteAddr)
	{
		return bytes[byteAddr] & 0xFF;
	}
	public void writeByte(int byteAddr, int val)
	{
		bytes[byteAddr] = (byte) val;
	}
	public void writeWord(int byteAddr, int val)
	{
		bytes[byteAddr] = (byte) (val >> 8);
		bytes[byteAddr + 1] = (byte) val;
	}
}