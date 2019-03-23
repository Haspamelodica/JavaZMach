package net.haspamelodica.javaz;

public class SequentialMemoryAccess
{
	private final Memory	mem;
	private int				addr;

	public SequentialMemoryAccess(Memory mem)
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
}