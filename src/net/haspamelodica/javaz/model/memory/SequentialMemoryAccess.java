package net.haspamelodica.javaz.model.memory;

public class SequentialMemoryAccess
{
	private final ReadOnlyMemory	mem;
	private int						addr;

	public SequentialMemoryAccess(ReadOnlyMemory mem)
	{
		this.mem = mem;
	}

	public void setAddress(int addr)
	{
		this.addr = addr;
	}
	public int getAddress()
	{
		return this.addr;
	}

	public int readNextByte()
	{
		return mem.readByte(addr ++);
	}
	public int readNextWord()
	{
		int word = mem.readWord(addr);
		addr += 2;
		return word;
	}
	public void skipBytes(int bytes)
	{
		addr += bytes;
	}
	public void skipWords(int words)
	{
		addr += words << 1;
	}
}