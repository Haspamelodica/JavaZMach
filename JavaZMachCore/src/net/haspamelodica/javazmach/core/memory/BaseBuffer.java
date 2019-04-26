package net.haspamelodica.javazmach.core.memory;

public class BaseBuffer
{
	protected final ReadOnlyMemory mem;

	protected boolean	zeroTerminated;
	protected int		entryByteSize;
	protected int		baseAddr;
	protected int		dataStartAddr;

	protected int addr;

	public BaseBuffer(ReadOnlyMemory mem)
	{
		this.mem = mem;
	}

	public void reset(int baseAddr, boolean zeroTerminated, int entryByteSize)
	{
		if(entryByteSize < 1)
			throw new IllegalArgumentException("Illegal entry size: " + entryByteSize);
		this.zeroTerminated = zeroTerminated;
		this.entryByteSize = entryByteSize;
		this.baseAddr = baseAddr;
		this.dataStartAddr = baseAddr + (zeroTerminated ? 1 : 2);

		this.addr = dataStartAddr;
	}
	public int getCapacity()
	{
		int b = mem.readByte(baseAddr);
		return zeroTerminated ? b - 1 : b;
	}

	protected int getRelativeAddress(int off)
	{
		return addr + off;
	}
	protected void advanceToNextEntryUnchecked()
	{
		addr += entryByteSize;
	}
	protected void advanceToLastEntryUnchecked()
	{
		addr -= entryByteSize;
	}
}