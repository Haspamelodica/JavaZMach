package net.haspamelodica.javaz.core.memory;

public class ReadOnlyBuffer extends BaseBuffer
{
	public ReadOnlyBuffer(ReadOnlyMemory mem)
	{
		super(mem);
	}

	public boolean hasNext()
	{
		return zeroTerminated ? mem.readByte(addr) != 0 : mem.readByte(baseAddr + 1) < addr - dataStartAddr;
	}
	public int readNextEntryByte(int off)
	{
		return mem.readByte(getRelativeAddress(off));
	}
	public int readNextEntryWord(int off)
	{
		return mem.readWord(getRelativeAddress(off));
	}
	public void finishEntry()
	{
		if(!hasNext())
			throw new MemoryException("Buffer underflow");
		advanceToNextEntryUnchecked();
	}
}