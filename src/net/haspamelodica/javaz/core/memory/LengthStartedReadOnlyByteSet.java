package net.haspamelodica.javaz.core.memory;

public class LengthStartedReadOnlyByteSet implements ReadOnlyByteSet
{
	private final ReadOnlyMemory mem;

	private final boolean lengthIsWord;

	private int	startAddr;
	private int	firstAddrAfterSet;

	public LengthStartedReadOnlyByteSet(ReadOnlyMemory mem, boolean lengthIsWord)
	{
		this.mem = mem;
		this.lengthIsWord = lengthIsWord;
	}

	/**
	 * A value of -1 means "pseudo set containing nothing"
	 */
	public void setStartAddr(int startAddr)
	{
		this.startAddr = startAddr;
		if(startAddr >= 0)
			firstAddrAfterSet = startAddr + (lengthIsWord ? mem.readWord(startAddr) : mem.readByte(startAddr));
	}

	@Override
	public boolean contains(int val)
	{
		if(startAddr == -1)
			return false;
		for(int addr = startAddr; addr < firstAddrAfterSet; addr ++)
			if(mem.readByte(addr) == val)
				return true;
		return false;
	}
}