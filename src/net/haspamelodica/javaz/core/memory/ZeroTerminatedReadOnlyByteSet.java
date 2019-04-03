package net.haspamelodica.javaz.core.memory;

public class ZeroTerminatedReadOnlyByteSet implements ReadOnlyByteSet
{
	private final ReadOnlyMemory mem;

	private int startAddr;

	public ZeroTerminatedReadOnlyByteSet(ReadOnlyMemory mem)
	{
		this.mem = mem;
	}

	public void setStartAddr(int startAddr)
	{
		this.startAddr = startAddr;
	}

	@Override
	public boolean contains(int val)
	{
		for(int addr = startAddr;; addr ++)
		{
			int b = mem.readByte(addr);
			if(b == val)
				return true;
			else if(b == 0)
				return false;
		}
	}
}