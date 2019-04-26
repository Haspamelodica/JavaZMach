package net.haspamelodica.javazmach.core.memory;

public class WritableUndoableBuffer extends WritableBuffer
{
	private final WritableMemory mem;

	public WritableUndoableBuffer(WritableMemory mem)
	{
		super(mem);
		this.mem = mem;
	}

	public boolean isEmpty()
	{
		return addr - dataStartAddr <= 0;
	}
	public int readLastEntryByte(int off)
	{
		return mem.readByte(getRelativeAddress(off) - entryByteSize);
	}
	public int readLastEntryWord(int off)
	{
		return mem.readWord(getRelativeAddress(off) - entryByteSize);
	}
	public void undoLastEntry()
	{
		if(isEmpty())
			throw new MemoryException("Buffer underflow");
		advanceToLastEntryUnchecked();
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