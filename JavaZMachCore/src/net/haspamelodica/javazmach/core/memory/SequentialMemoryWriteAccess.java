package net.haspamelodica.javazmach.core.memory;

public class SequentialMemoryWriteAccess
{
	private final WritableMemory	mem;
	protected int					addr;

	public SequentialMemoryWriteAccess(WritableMemory mem)
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

	public void writeNextByte(int value)
	{
		mem.writeByte(addr ++, value);
	}
	public void writeNextWord(int value)
	{
		mem.writeWord(addr, value);
		addr += 2;
	}
	public void writeNextBytes(byte[] data)
	{
		mem.writeNBytes(addr, data);
		addr += data.length;
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

	public void alignToBytes(int bytes)
	{
		//TODO which default pad byte?
		alignToBytes(bytes, 0x00);
	}
	public void alignToBytes(int bytes, int padValue)
	{
		// Conecptually VERY inefficient, but in practice probably not too bad, considering how small alignments are in practice.
		while(addr % bytes != 0)
			writeNextByte(padValue);
	}
}