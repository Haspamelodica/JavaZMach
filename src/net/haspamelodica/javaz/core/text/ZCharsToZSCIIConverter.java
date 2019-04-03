package net.haspamelodica.javaz.core.text;

import static net.haspamelodica.javaz.core.HeaderParser.AbbrevTableLocLoc;
import static net.haspamelodica.javaz.core.HeaderParser.AlphabetTableLocLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.SequentialMemoryAccess;

public class ZCharsToZSCIIConverter implements ZSCIICharStream
{
	/** <code>"abcdefghijklmnopqrstuvwxyz"</code> */
	private static final int[]	defaultA0		= {0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A};
	/** <code>"ABCDEFGHIJKLMNOPQRSTUVWXYZ"</code> */
	private static final int[]	defaultA1		= {0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A};
	/** <code>"^0123456789.,!?_#'"/\<-:()"</code> ^ means "not parsed by this table" */
	private static final int[]	defaultA2_V1	= {0xFF, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x2E, 0x2C, 0x21, 0x3F, 0x5F, 0x23, 0x27, 0x22, 0x2F, 0x5C, 0x3C, 0x2D, 0x3A, 0x28, 0x29};
	/** <code>"^^0123456789.,!?_#'"/\-:()"</code> ^ means "not parsed by this table" */
	private static final int[]	defaultA2_other	= {0xFF, 0xFF, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x2E, 0x2C, 0x21, 0x3F, 0x5F, 0x23, 0x27, 0x22, 0x2F, 0x5C, 0x2D, 0x3A, 0x28, 0x29};

	private final int version;

	private final boolean	dontAllowNestedAbbreviations;
	private final boolean	checkAbbreviationsDontStopIncomplete;
	private final boolean	checkZSCIIRange;

	private final HeaderParser		headerParser;
	private final ReadOnlyMemory	mem;
	private final ZCharStreamSource	source;

	private final SequentialMemoryAccess	abbrevSeqMem;
	private final ZCharStreamSource			abbrevSource;

	private int	abbreviationsTableLoc;
	private int	alphabetTableLoc;

	public ZCharsToZSCIIConverter(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, ZCharStreamSource source)
	{
		this.version = version;

		this.dontAllowNestedAbbreviations = config.getBool("text.zchars.abbreviations.dont_allow_nesting");
		this.checkAbbreviationsDontStopIncomplete = config.getBool("text.zchars.abbreviations.check_no_incomplete_construction_stop");
		this.checkZSCIIRange = config.getBool("text.zscii.check_10bit_range");

		this.headerParser = headerParser;
		this.mem = mem;
		this.source = source;

		this.abbrevSeqMem = new SequentialMemoryAccess(mem);
		this.abbrevSource = new ZCharsSeqMemUnpacker(abbrevSeqMem);
	}
	public void reset()
	{
		if(version > 1)
			abbreviationsTableLoc = headerParser.getField(AbbrevTableLocLoc);
		if(version > 4)
			alphabetTableLoc = headerParser.getField(AlphabetTableLocLoc);
	}

	@Override
	public void decode(ZSCIICharStreamReceiver target)
	{
		decode(target, source, false);
	}
	private void decode(ZSCIICharStreamReceiver target, ZCharStreamSource source, boolean isAbbreviation)
	{
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
					decode(target, abbrevSource, true);
					abbrevSeqMem.setAddress(oldAbbrevSourceAddress);
					state = 5;
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
							target.accept(translateZCharToZSCII(zChar, alphabetCurrent));
							break;
					}
					break;
			}
		} while(source.hasNext());
		if(isAbbreviation && checkAbbreviationsDontStopIncomplete && state != 5)
			throw new TextException("Abbreviation ended with an incomplete multi-Z-char construction");
	}
	private int translateZCharToZSCII(byte zChar, int alphabet)
	{
		zChar -= 6;
		if(alphabetTableLoc == 0)
			switch(alphabet)
			{
				case 0:
					return defaultA0[zChar];
				case 1:
					return defaultA1[zChar];
				case 2:
					if(version == 1)
						return defaultA2_V1[zChar];
					else
						return defaultA2_other[zChar];
				default:
					throw new IllegalStateException("Illegal alphabet: " + alphabet);
			}
		int zscii = mem.readWord(alphabetTableLoc + (zChar << 1) + (alphabet * 26));
		if(zscii > 0x3FF && checkZSCIIRange)
			throw new TextException("Out-of-range ZSCII char: " + zscii);
		return zscii;
	}
}