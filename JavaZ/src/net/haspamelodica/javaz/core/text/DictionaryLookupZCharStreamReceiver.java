package net.haspamelodica.javaz.core.text;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;

public class DictionaryLookupZCharStreamReceiver implements ZCharStreamReceiver
{
	private final int version;

	private final boolean checkEntryLength;

	private final ReadOnlyMemory mem;

	private int	entryLength;
	private int	entryNumber;
	private int	receivedZCharsSoFar;
	/**
	 * Contains the address of the first (textual) word in the current dictionary starting with the Z-chars received so far.
	 * When no Z-chars have been received, contains the address of the first (textual) word.
	 */
	private int	currentWordAddress;
	private int	entryIndex;

	public DictionaryLookupZCharStreamReceiver(GlobalConfig config, int version, ReadOnlyMemory mem)
	{
		this.version = version;

		this.checkEntryLength = config.getBool("text.tokenise.dictionary.check_entry_length");

		this.mem = mem;
	}
	public void reset(int dictionaryAddr)
	{
		int wordSeparatorsCount = mem.readByte(dictionaryAddr);
		entryLength = mem.readByte(dictionaryAddr + wordSeparatorsCount + 1);
		if(checkEntryLength && entryLength < (version > 4 ? 6 : 4))
			throw new TextException("Illegal dictionary entry length: " + entryLength);
		entryNumber = mem.readWord(dictionaryAddr + wordSeparatorsCount + 2);
		receivedZCharsSoFar = 0;
		currentWordAddress = dictionaryAddr + wordSeparatorsCount + 4;
		entryIndex = 0;
	}

	private static final int[]	zCharsPerWordMasks	= {0x7C_00, 0x03_E0, 0x00_1F};
	private static final int[]	zCharsPerWordShifts	= {10, 5, 0};
	@Override
	public void accept(byte zChar)
	{
		if(receivedZCharsSoFar < (version > 3 ? 9 : 6))
		{
			if(currentWordAddress != -1)
			{
				int off = (receivedZCharsSoFar / 3) << 1;
				int mask = zCharsPerWordMasks[receivedZCharsSoFar % 3];
				int shift = zCharsPerWordShifts[receivedZCharsSoFar % 3];
				//TODO make this faster
				for(;;)
				{
					int dictZChar = (mem.readWord(currentWordAddress + off) & mask) >>> shift;
					if(dictZChar > zChar)
					{
						currentWordAddress = -1;
						break;
					} else if(dictZChar == zChar)
						break;
					entryIndex ++;
					if(entryIndex >= entryNumber)
					{
						currentWordAddress = -1;
						break;
					}
					currentWordAddress += entryLength;
				}
			}
			receivedZCharsSoFar ++;
		}
	}
	public int getWordLengthSoFar()
	{
		return receivedZCharsSoFar;
	}
	public int finishAndGetWordAddr()
	{
		while(receivedZCharsSoFar < (version > 3 ? 9 : 6))
			accept((byte) 5);
		return currentWordAddress;
	}
}