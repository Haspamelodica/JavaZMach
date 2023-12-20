package net.haspamelodica.javazmach.core.memory;

public interface WritableMemory extends ReadOnlyMemory
{
	public void writeByte(int byteAddr, int val);
	public default void writeWord(int byteAddr, int val)
	{
		writeByte(byteAddr + 0, (val >> 0x08) & 0xff);
		writeByte(byteAddr + 1, (val >> 0x00) & 0xff);
	}
	public default void writeInt(int byteAddr, int val)
	{
		writeByte(byteAddr + 0, (val >> 0x18) & 0xff);
		writeByte(byteAddr + 1, (val >> 0x10) & 0xff);
		writeByte(byteAddr + 2, (val >> 0x08) & 0xff);
		writeByte(byteAddr + 3, (val >> 0x00) & 0xff);
	}
	public default void writeLong(int byteAddr, long val)
	{
		writeByte(byteAddr + 0, (int) (val >> 0x38) & 0xff);
		writeByte(byteAddr + 1, (int) (val >> 0x30) & 0xff);
		writeByte(byteAddr + 2, (int) (val >> 0x28) & 0xff);
		writeByte(byteAddr + 3, (int) (val >> 0x20) & 0xff);
		writeByte(byteAddr + 4, (int) (val >> 0x18) & 0xff);
		writeByte(byteAddr + 5, (int) (val >> 0x10) & 0xff);
		writeByte(byteAddr + 6, (int) (val >> 0x08) & 0xff);
		writeByte(byteAddr + 7, (int) (val >> 0x00) & 0xff);
	}

	public default void writeNBytes(int byteAddr, byte[] data, int off, int len)
	{
		for(int i = 0; i < len; i ++)
			writeByte(byteAddr + i, data[off + i]);
	}
	public default void writeNBytes(int byteAddr, byte[] data)
	{
		writeNBytes(byteAddr, data, 0, data.length);
	}
}