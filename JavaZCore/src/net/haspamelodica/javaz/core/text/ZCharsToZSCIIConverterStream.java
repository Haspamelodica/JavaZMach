package net.haspamelodica.javaz.core.text;

import static net.haspamelodica.javaz.core.header.HeaderField.AbbrevTableLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.header.HeaderParser;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.SequentialMemoryAccess;

public class ZCharsToZSCIIConverterStream implements ZSCIICharStream
{
	private final int version;

	private final boolean	dontAllowNestedAbbreviations;
	private final boolean	checkAbbreviationsDontStopIncomplete;

	private final HeaderParser			headerParser;
	private final ReadOnlyMemory		mem;
	private final ZCharsAlphabet		alphabet;
	private final ZCharStreamReceiver	zCharsTarget;

	private final SequentialMemoryAccess	abbrevSeqMem;
	private final ZCharStream				abbrevStream;

	private ZCharStream				source;
	private ZSCIICharStreamReceiver	target;
	private int						abbreviationsTableLoc;

	/**
	 * 0: abbreviation with z-char 1 follows
	 * 1: abbreviation with z-char 2 follows
	 * 2: abbreviation with z-char 3 follows
	 * 3: first half of ZSCII char follows
	 * 4: second half of ZSCII char follows
	 * 5: no special state
	 */
	int		state;
	int		alphabetLock;
	int		nextAlphabetCurrent;
	int		zsciiCharTopHalf;
	boolean	isAbbreviation;

	public ZCharsToZSCIIConverterStream(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, ZCharsAlphabet alphabet)
	{
		this.version = version;

		this.dontAllowNestedAbbreviations = config.getBool("text.zchars.abbreviations.dont_allow_nesting");
		this.checkAbbreviationsDontStopIncomplete = config.getBool("text.zchars.abbreviations.check_no_incomplete_construction_stop");

		this.headerParser = headerParser;
		this.mem = mem;
		this.alphabet = alphabet;
		this.zCharsTarget = this::decodeNextChar;

		this.abbrevSeqMem = new SequentialMemoryAccess(mem);
		this.abbrevStream = new ZCharsSeqMemUnpacker(abbrevSeqMem);
	}
	public void reset(ZCharStream source)
	{
		this.source = source;
		if(version > 1)
			abbreviationsTableLoc = headerParser.getField(AbbrevTableLoc);
	}

	@Override
	public void decode(ZSCIICharStreamReceiver target)
	{
		this.isAbbreviation = false;
		this.target = target;
		decode(source);
	}
	private void decode(ZCharStream source)
	{
		state = 5;
		alphabetLock = 0;
		nextAlphabetCurrent = 0;
		source.decode(zCharsTarget);
	}
	private void decodeNextChar(byte zChar)
	{
		int alphabetCurrent = nextAlphabetCurrent;
		nextAlphabetCurrent = alphabetLock;
		switch(state)
		{
			case 0:
			case 1:
			case 2:
				int oldAbbrevAddr = abbrevSeqMem.getAddress();
				boolean oldIsAbbreviation = isAbbreviation;
				int oldAlphabetLock = alphabetLock;
				int oldNextAlphabetCurrent = nextAlphabetCurrent;
				//abbreviation string starts at word address at word 32(z-1)+x in the abbrevations table,
				//where z is the first and x the second z-char.
				abbrevSeqMem.setAddress(mem.readWord(abbreviationsTableLoc + (zChar << 1) + (state << 6)) << 1);
				isAbbreviation = true;
				decode(abbrevStream);
				abbrevSeqMem.setAddress(oldAbbrevAddr);
				isAbbreviation = oldIsAbbreviation;
				if(checkAbbreviationsDontStopIncomplete && state != 5)
					throw new TextException("Abbreviation ended with an incomplete multi-Z-char construction");
				state = 5;
				alphabetLock = oldAlphabetLock;
				nextAlphabetCurrent = oldNextAlphabetCurrent;
				break;
			case 3:
				zsciiCharTopHalf = zChar << 5;
				state = 4;
				break;
			case 4:
				target.accept(zsciiCharTopHalf + zChar);
				state = 5;
				break;
			case 5:
				switch(zChar)
				{
					case 0:
						target.accept(32);//space
						break;
					case 1:
						if(version < 2)
							target.accept(13);//newline
						else if(isAbbreviation && dontAllowNestedAbbreviations)
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
								break;
							}
						}
						//intentional fall-through
					default:
						target.accept(alphabet.translateZCharToZSCII(zChar, alphabetCurrent));
						break;
				}
				break;
		}
	}
}