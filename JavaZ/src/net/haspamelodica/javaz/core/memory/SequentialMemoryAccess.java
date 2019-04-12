package net.haspamelodica.javaz.core.memory;

public class SequentialMemoryAccess
{
	private final ReadOnlyMemory	mem;
	protected int					addr;

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
	/**
	 * This method can "jump" backwards if a negative offset is supplied.
	 */
	public void skipBytes(int bytesOff)
	{
		addr += bytesOff;
	}
	/**
	 * This method can "jump" backwards if a negative offset is supplied.
	 */
	public void skipWords(int wordsOff)
	{
		addr += wordsOff << 1;
	}
}