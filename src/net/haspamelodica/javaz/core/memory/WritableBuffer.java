package net.haspamelodica.javaz.core.memory;

public class WritableBuffer extends BaseBuffer
{
	private final WritableMemory mem;

	public WritableBuffer(WritableMemory mem)
	{
		super(mem);
		this.mem = mem;
	}

	@Override
	public void reset(int baseAddr, boolean zeroTerminated, int entryByteSize)
	{
		super.reset(baseAddr, zeroTerminated, entryByteSize);
		writeLength();
	}
	public boolean isFull()
	{
		return getCapacity() * entryByteSize <= addr - dataStartAddr;
	}
	public void writeNextEntryByte(int off, int val)
	{
		mem.writeByte(getRelativeAddress(off), val);
	}
	public void writeNextEntryWord(int off, int val)
	{
		mem.writeWord(getRelativeAddress(off), val);
	}
	public void finishEntry()
	{
		if(isFull())
			throw new MemoryException("Buffer overflow");
		advanceToNextEntryUnchecked();
		writeLength();
	}
	private void writeLength()
	{
		if(zeroTerminated)
			mem.writeByte(addr, 0);
		else
			mem.writeByte(baseAddr + 1, (addr - dataStartAddr) / entryByteSize);
	}
}