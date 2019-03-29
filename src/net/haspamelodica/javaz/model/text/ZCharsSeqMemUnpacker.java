package net.haspamelodica.javaz.model.text;

import net.haspamelodica.javaz.model.memory.SequentialMemoryAccess;

public class ZCharsSeqMemUnpacker implements ZCharStreamSource
{
	private final SequentialMemoryAccess mem;

	private int	zCharInWordIndex;
	private int	currentZCharsWord;

	public ZCharsSeqMemUnpacker(SequentialMemoryAccess mem)
	{
		this.mem = mem;
	}

	@Override
	public void reset()
	{
		zCharInWordIndex = 0;
	}

	@Override
	public boolean hasNext()
	{
		return zCharInWordIndex != 2 || (currentZCharsWord & 0x80_00) == 0;
	}
	private static final int[]	zCharsPerWordMasks	= {0x7C_00, 0x03_E0, 0x00_1F};
	private static final int[]	zCharsPerWordShifts	= {10, 5, 0};
	@Override
	public byte nextZChar()
	{
		if(zCharInWordIndex == 0)
			currentZCharsWord = mem.readNextWord();
		int zChar = (currentZCharsWord & zCharsPerWordMasks[zCharInWordIndex]) >>> zCharsPerWordShifts[zCharInWordIndex];
		zCharInWordIndex = (zCharInWordIndex + 1) % 3;
		return (byte) zChar;
	}

}