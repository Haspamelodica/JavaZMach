package net.haspamelodica.javaz.core.text;

import static net.haspamelodica.javaz.core.HeaderParser.DictionaryLocLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.memory.ReadOnlyBuffer;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.WritableBuffer;

public class Tokeniser
{
	private final int version;

	private final HeaderParser							headerParser;
	private final ReadOnlyMemory						mem;
	private final ZCharsAlphabet						alphabet;
	private final DictionaryLookupZCharStreamReceiver	dictLookupZRecv;

	private int defaultDictionary;

	public Tokeniser(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, ZCharsAlphabet alphabet)
	{
		this.version = version;

		this.headerParser = headerParser;
		this.mem = mem;
		this.alphabet = alphabet;
		this.dictLookupZRecv = new DictionaryLookupZCharStreamReceiver(config, version, mem);
	}
	public void reset()
	{
		this.defaultDictionary = headerParser.getField(DictionaryLocLoc);
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
		//TODO
	}
}