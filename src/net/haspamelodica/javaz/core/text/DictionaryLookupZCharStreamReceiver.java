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
		currentWordAddress = dictionaryAddr + wordSeparatorsCount + 3;
	}

	@Override
	public void accept(byte zChar)
	{
		//TODO
	}
	public int finishAndGetWordAddr()
	{
		//TODO
	}
}