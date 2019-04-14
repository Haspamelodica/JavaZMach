package net.haspamelodica.javazmach.core.memory;

import java.util.Arrays;

public class CopyOnWriteMemory implements WritableMemory
{
	private static final int	BLOCK_LENGTH_BITS		= 4;
	private static final int	BLOCK_LENGTH			= 1 << BLOCK_LENGTH_BITS;
	private static final int	BLOCK_OFF_MASK			= BLOCK_LENGTH - 1;
	private static final int	BLOCK_ARRAY_OVERHEAD	= 10;

	private final ReadOnlyMemory unchangedMem;

	private int[]		writtenBlockNumbers;
	private int			writtenBlockCount;
	private byte[][]	writtenBlocks;

	public CopyOnWriteMemory(ReadOnlyMemory mem)
	{
		this.unchangedMem = mem;
		this.writtenBlockNumbers = new int[BLOCK_ARRAY_OVERHEAD];
		this.writtenBlocks = new byte[BLOCK_ARRAY_OVERHEAD][BLOCK_LENGTH];
	}
	public void reset()
	{
		writtenBlockCount = 0;
	}

	@Override
	public int getSize()
	{
		return unchangedMem.getSize();
	}
	@Override
	public int readByte(int byteAddr)
	{
		//TODO make this faster
		for(int i = 0; i < writtenBlockCount; i ++)
			if(writtenBlockNumbers[i] == byteAddr >>> BLOCK_LENGTH_BITS)
				return writtenBlocks[i][byteAddr & BLOCK_OFF_MASK] & 0xFF;
		return unchangedMem.readByte(byteAddr);
	}
	@Override
	public int readWord(int byteAddr)
	{
		return (readByte(byteAddr) << 8) | readByte(byteAddr + 1);
	}
	@Override
	public void writeByte(int byteAddr, int val)
	{
		int blockIndex = makeOrGetBlockIndex(byteAddr);
		writtenBlocks[blockIndex][byteAddr & BLOCK_OFF_MASK] = (byte) val;
	}
	@Override
	public void writeWord(int byteAddr, int val)
	{
		writeByte(byteAddr, val >>> 8);
		writeByte(byteAddr + 1, val);
	}

	private int makeOrGetBlockIndex(int byteAddr)
	{
		int blockNumber = byteAddr >>> BLOCK_LENGTH_BITS;
		int blockInsertionIndex = writtenBlockCount;
		//TODO make this faster
		for(int i = 0; i < writtenBlockCount; i ++)
			if(writtenBlockNumbers[i] >= blockNumber)
			{
				if(writtenBlockNumbers[i] == blockNumber)
					return i;
				blockInsertionIndex = i;
				break;
			}
		if(writtenBlockCount == writtenBlockNumbers.length)
		{
			int newLength = writtenBlockCount + BLOCK_ARRAY_OVERHEAD;
			writtenBlockNumbers = Arrays.copyOf(writtenBlockNumbers, newLength);
			writtenBlocks = Arrays.copyOf(writtenBlocks, newLength);
			for(int i = writtenBlockCount; i < newLength; i ++)
				writtenBlocks[i] = new byte[BLOCK_LENGTH];
		}
		byte[] blockData = writtenBlocks[writtenBlockCount];
		int baseAddr = blockNumber << BLOCK_LENGTH_BITS;
		for(int i = 0; i < BLOCK_LENGTH; i ++)
			blockData[i] = (byte) unchangedMem.readByte(baseAddr + i);
		int blocksToCopy = writtenBlockCount - blockInsertionIndex;
		System.arraycopy(writtenBlockNumbers, blockInsertionIndex, writtenBlockNumbers, blockInsertionIndex + 1, blocksToCopy);
		System.arraycopy(writtenBlocks, blockInsertionIndex, writtenBlocks, blockInsertionIndex + 1, blocksToCopy);
		writtenBlockNumbers[blockInsertionIndex] = blockNumber;
		writtenBlocks[blockInsertionIndex] = blockData;
		writtenBlockCount ++;
		return blockInsertionIndex;
	}
}