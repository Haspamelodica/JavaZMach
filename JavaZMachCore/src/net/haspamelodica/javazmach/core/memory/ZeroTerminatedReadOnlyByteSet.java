package net.haspamelodica.javazmach.core.memory;

public class ZeroTerminatedReadOnlyByteSet implements ReadOnlyByteSet
{
	private final ReadOnlyMemory mem;

	private int startAddr;

	public ZeroTerminatedReadOnlyByteSet(ReadOnlyMemory mem)
	{
		this.mem = mem;
	}

	/**
	 * A value of -1 means "pseudo set containing nothing"
	 */
	public void setStartAddr(int startAddr)
	{
		this.startAddr = startAddr;
	}

	@Override
	public boolean contains(int val)
	{
		if(startAddr == -1)
			return false;
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