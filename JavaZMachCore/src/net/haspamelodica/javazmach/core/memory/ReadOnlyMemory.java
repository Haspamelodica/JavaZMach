package net.haspamelodica.javazmach.core.memory;

public interface ReadOnlyMemory
{
	public int getSize();
	public int readByte(int byteAddr);
	public default int readWord(int byteAddr)
	{
		return 0
				| ((readByte(byteAddr + 0) & 0xff) << 0x08)
				| ((readByte(byteAddr + 1) & 0xff) << 0x00);
	}
	public default int readInt(int byteAddr)
	{
		return 0
				| ((readByte(byteAddr + 0) & 0xff) << 0x18)
				| ((readByte(byteAddr + 1) & 0xff) << 0x10)
				| ((readByte(byteAddr + 2) & 0xff) << 0x08)
				| ((readByte(byteAddr + 3) & 0xff) << 0x00);
	}
	public default long readLong(int byteAddr)
	{
		return 0
				| ((readByte(byteAddr + 0) & 0xffL) << 0x38)
				| ((readByte(byteAddr + 1) & 0xffL) << 0x30)
				| ((readByte(byteAddr + 2) & 0xffL) << 0x28)
				| ((readByte(byteAddr + 3) & 0xffL) << 0x20)
				| ((readByte(byteAddr + 4) & 0xffL) << 0x18)
				| ((readByte(byteAddr + 5) & 0xffL) << 0x10)
				| ((readByte(byteAddr + 6) & 0xffL) << 0x08)
				| ((readByte(byteAddr + 7) & 0xffL) << 0x00);
	}

	public default void readNBytes(int byteAddr, byte[] data, int off, int len)
	{
		for(int i = 0; i < len; i ++)
			data[off + i] = (byte) readByte(byteAddr + i);
	}
	public default void readNBytes(int byteAddr, byte[] data)
	{
		readNBytes(byteAddr, data, 0, 0);
	}
	public default byte[] readNBytes(int byteAddr, int len)
	{
		byte[] data = new byte[len];
		readNBytes(byteAddr, data, 0, len);
		return data;
	}
}