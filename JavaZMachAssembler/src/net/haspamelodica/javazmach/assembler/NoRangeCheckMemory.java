package net.haspamelodica.javazmach.assembler;

import java.util.Arrays;

import net.haspamelodica.javazmach.core.memory.WritableMemory;

public class NoRangeCheckMemory implements WritableMemory
{
	private byte[] data;

	public NoRangeCheckMemory()
	{
		this.data = new byte[0];
	}

	@Override
	public int getSize()
	{
		throw new UnsupportedOperationException("this memory has no size");
	}

	@Override
	public int readByte(int byteAddr)
	{
		return byteAddr >= data.length ? 0 : data[byteAddr] & 0xff;
	}

	@Override
	public void writeByte(int byteAddr, int val)
	{
		if(byteAddr >= data.length)
			data = Arrays.copyOf(data, byteAddr + 1);
		data[byteAddr] = (byte) val;
	}

	@Override
	public void writeWord(int byteAddr, int val)
	{
		// write upper byte first to avoid growing array twice if this word is new end of memory
		writeByte(byteAddr + 1, val >> 8);
		writeByte(byteAddr, val);
	}

	public byte[] data()
	{
		return data;
	}

	// not overriding getSize because JavaZMach stuff might assume getSize to be constant,
	// but this changes
	public int currentSize()
	{
		return data.length;
	}
}
