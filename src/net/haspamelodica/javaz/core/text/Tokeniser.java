package net.haspamelodica.javaz.core.text;

import static net.haspamelodica.javaz.core.header.HeaderField.DictionaryLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.header.HeaderParser;
import net.haspamelodica.javaz.core.memory.LengthStartedReadOnlyByteSet;
import net.haspamelodica.javaz.core.memory.ReadOnlyBuffer;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.WritableBuffer;

public class Tokeniser
{
	private final HeaderParser							headerParser;
	private final ZCharsAlphabet						alphabet;
	private final DictionaryLookupZCharStreamReceiver	dictLookupZRecv;
	private final LengthStartedReadOnlyByteSet			wordSeparatorSet;

	private int defaultDictionary;

	public Tokeniser(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, ZCharsAlphabet alphabet)
	{
		this.headerParser = headerParser;
		this.alphabet = alphabet;
		this.dictLookupZRecv = new DictionaryLookupZCharStreamReceiver(config, version, mem);
		this.wordSeparatorSet = new LengthStartedReadOnlyByteSet(mem, false);
	}
	public void reset()
	{
		this.defaultDictionary = headerParser.getField(DictionaryLoc);
	}

	public void tokenise(ReadOnlyBuffer textBuf, WritableBuffer targetBuf)
	{
		tokenise(textBuf, targetBuf, 0);
	}
	public void tokenise(ReadOnlyBuffer textBuf, WritableBuffer targetBuf, int dictionary)
	{
		tokenise(textBuf, targetBuf, dictionary, false);
	}
	public void tokenise(ReadOnlyBuffer textBuf, WritableBuffer targetBuf, int dictionary, boolean skipWordsNotInDict)
	{
		if(dictionary == 0)
			dictionary = defaultDictionary;
		int textBufOff = 1;
		int textBufOffFirstLetter = -1;
		int zsciiLen = 0;
		wordSeparatorSet.setStartAddr(dictionary);
		dictLookupZRecv.reset(dictionary);

		while(textBuf.hasNext())
		{
			int zsciiChar = textBuf.readNextEntryByte(0);
			textBuf.finishEntry();
			if(wordSeparatorSet.contains(zsciiChar))
			{
				writeParsedResultAndResetDictLookup(textBufOffFirstLetter, zsciiLen, skipWordsNotInDict, targetBuf, dictionary);
				alphabet.translateZSCIIToZChars(zsciiChar, dictLookupZRecv);
				zsciiLen = 1;
				writeParsedResultAndResetDictLookup(textBufOffFirstLetter, zsciiLen, skipWordsNotInDict, targetBuf, dictionary);
				zsciiLen = 0;
			} else if(zsciiChar == 32)
			{
				writeParsedResultAndResetDictLookup(textBufOffFirstLetter, zsciiLen, skipWordsNotInDict, targetBuf, dictionary);
				zsciiLen = 0;
			} else
			{
				if(zsciiLen == 0)
					textBufOffFirstLetter = textBufOff;
				alphabet.translateZSCIIToZChars(zsciiChar, dictLookupZRecv);
				zsciiLen ++;
			}
			textBufOff ++;
		}
		writeParsedResultAndResetDictLookup(textBufOffFirstLetter, zsciiLen, skipWordsNotInDict, targetBuf, dictionary);
	}
	private void writeParsedResultAndResetDictLookup(int textBufOffFirstLetter, int zsciiLen, boolean skipWordsNotInDict, WritableBuffer targetBuf, int dictionary)
	{
		if(zsciiLen > 0)
		{
			int wordAddr = dictLookupZRecv.finishAndGetWordAddr();
			if(wordAddr >= 0 || !skipWordsNotInDict)
			{
				targetBuf.writeNextEntryWord(0, wordAddr < 0 ? 0 : wordAddr);
				targetBuf.writeNextEntryByte(2, zsciiLen);
				targetBuf.writeNextEntryByte(3, textBufOffFirstLetter);
				targetBuf.finishEntry();
			}
			dictLookupZRecv.reset(dictionary);
		}
	}
}