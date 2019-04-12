package net.haspamelodica.javaz.core.text;

import static net.haspamelodica.javaz.core.header.HeaderField.AbbrevTableLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.header.HeaderParser;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.SequentialMemoryAccess;

public class ZCharsToZSCIIConverter implements ZSCIICharStream
{
	private final int version;

	private final boolean	dontAllowNestedAbbreviations;
	private final boolean	checkAbbreviationsDontStopIncomplete;

	private final HeaderParser		headerParser;
	private final ReadOnlyMemory	mem;
	private final ZCharsAlphabet	alphabet;
	private final ZCharStreamSource	source;

	private final SequentialMemoryAccess	abbrevSeqMem;
	private final ZCharStreamSource			abbrevSource;

	private int abbreviationsTableLoc;

	public ZCharsToZSCIIConverter(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, ZCharsAlphabet alphabet, ZCharStreamSource source)
	{
		this.version = version;

		this.dontAllowNestedAbbreviations = config.getBool("text.zchars.abbreviations.dont_allow_nesting");
		this.checkAbbreviationsDontStopIncomplete = config.getBool("text.zchars.abbreviations.check_no_incomplete_construction_stop");

		this.headerParser = headerParser;
		this.mem = mem;
		this.alphabet = alphabet;
		this.source = source;

		this.abbrevSeqMem = new SequentialMemoryAccess(mem);
		this.abbrevSource = new ZCharsSeqMemUnpacker(abbrevSeqMem);
	}
	public void reset()
	{
		if(version > 1)
			abbreviationsTableLoc = headerParser.getField(AbbrevTableLoc);
	}

	@Override
	public int decode(ZSCIICharStreamReceiver target)
	{
		return decode(target, source, false);
	}
	private int decode(ZSCIICharStreamReceiver target, ZCharStreamSource source, boolean isAbbreviation)
	{
		int chars = 0;
		/**
		 * 0: abbreviation with z-char 1 follows
		 * 1: abbreviation with z-char 2 follows
		 * 2: abbreviation with z-char 3 follows
		 * 3: first half of ZSCII char follows
		 * 4: second half of ZSCII char follows
		 * 5: no special state
		 */
		int state = 5;
		int alphabetLock = 0;
		int nextAlphabetCurrent = 0;
		int zsciiCharTopHalf = 0;
		source.reset();
		do
		{
			int alphabetCurrent = nextAlphabetCurrent;
			nextAlphabetCurrent = alphabetLock;
			byte zChar = source.nextZChar();
			switch(state)
			{
				case 0:
				case 1:
				case 2:
					int oldAbbrevSourceAddress = abbrevSeqMem.getAddress();
					//abbreviation string starts at word address at word 32(z-1)+x in the abbrevations table,
					//where z is the first and x the second z-char.
					abbrevSeqMem.setAddress(mem.readWord(abbreviationsTableLoc + (zChar << 1) + (state << 6)) << 1);
					chars += decode(target, abbrevSource, true);
					abbrevSeqMem.setAddress(oldAbbrevSourceAddress);
					state = 5;
					break;
				case 3:
					zsciiCharTopHalf = zChar << 5;
					state = 4;
					break;
				case 4:
					target.accept(zsciiCharTopHalf + zChar);
					chars ++;
					state = 5;
					break;
				case 5:
					switch(zChar)
					{
						case 0:
							target.accept(32);//space
							chars ++;
							break;
						case 1:
							if(version < 2)
							{
								target.accept(13);//newline
								chars ++;
							} else if(isAbbreviation && dontAllowNestedAbbreviations)
								throw new TextException("Nested abbreviation");
							else
								state = 0;
							break;
						case 2:
						case 3:
							if(version < 3)
								nextAlphabetCurrent = (alphabetCurrent + zChar - 1) % 3;
							else if(isAbbreviation && dontAllowNestedAbbreviations)
								throw new TextException("Nested abbreviation");
							else
								state = zChar - 1;
							break;
						case 4:
						case 5:
							nextAlphabetCurrent = (alphabetCurrent + zChar - 3) % 3;
							if(version < 3)
								alphabetLock = nextAlphabetCurrent;
							break;
						case 6:
						case 7:
							if(alphabetCurrent == 2)
							{
								if(zChar == 6)
								{
									state = 3;
									break;
								} else if(version > 1)
								{
									target.accept(13);
									chars ++;
									break;
								}
							}
							//intentional fall-through
						default:
							target.accept(alphabet.translateZCharToZSCII(zChar, alphabetCurrent));
							chars ++;
							break;
					}
					break;
			}
		} while(source.hasNext());
		if(isAbbreviation && checkAbbreviationsDontStopIncomplete && state != 5)
			throw new TextException("Abbreviation ended with an incomplete multi-Z-char construction");
		return chars;
	}
}