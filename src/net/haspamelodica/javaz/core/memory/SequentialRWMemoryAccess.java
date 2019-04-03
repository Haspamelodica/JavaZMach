package net.haspamelodica.javaz.core.memory;

public class SequentialRWMemoryAccess extends SequentialMemoryAccess
{
	private final WritableMemory mem;

	public SequentialRWMemoryAccess(WritableMemory mem)
	{
		super(mem);
		this.mem = mem;
	}

	public void writeNextByte(int val)
	{
		mem.writeByte(addr ++, val);
	}
	public void writeNextWord(int val)
	{
		mem.writeWord(addr, val);
		addr += 2;
	}
}