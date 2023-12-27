package net.haspamelodica.javazmach.assembler.core;

import java.util.Arrays;

import net.haspamelodica.javazmach.core.memory.WritableMemory;

public class NoRangeCheckMemory implements WritableMemory
{
	private byte[] data;

	public NoRangeCheckMemory()
	{
		clear();
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

	public void clear()
	{
		data = new byte[0];
	}

	public void insertByte(int relAddr, int val)
	{
		byte[] oldData = data;
		data = new byte[oldData.length + 1];
		System.arraycopy(oldData, 0, data, 0, relAddr);
		data[relAddr] = (byte) val;
		System.arraycopy(oldData, relAddr, data, relAddr + 1, oldData.length - relAddr);
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
