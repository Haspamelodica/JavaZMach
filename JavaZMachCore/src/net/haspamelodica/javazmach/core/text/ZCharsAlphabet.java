package net.haspamelodica.javazmach.core.text;

import static net.haspamelodica.javazmach.core.header.HeaderField.AlphabetTableLoc;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.ReadOnlyMemory;

public class ZCharsAlphabet
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

	private final boolean checkZSCIIRange;

	private final HeaderParser		headerParser;
	private final ReadOnlyMemory	mem;

	private int alphabetTableLoc;

	public ZCharsAlphabet(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem)
	{
		this.version = version;

		this.checkZSCIIRange = config.getBool("text.zscii.check_10bit_range");

		this.headerParser = headerParser;
		this.mem = mem;
	}
	public void reset()
	{
		if(version > 4)
			alphabetTableLoc = headerParser.getField(AlphabetTableLoc);
	}

	public int translateZCharToZSCII(byte zChar, int alphabet)
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
	public void translateZSCIIToZChars(int zsciiChar, ZCharStreamReceiver target)
	{
		if(zsciiChar == 32)
			target.accept((byte) 0);
		if(zsciiChar == 13 && version == 1)
			target.accept((byte) 1);
		//TODO make the following faster
		for(byte zChar = 6; zChar < 32; zChar ++)
			if(translateZCharToZSCII(zChar, 0) == zsciiChar)
			{
				target.accept(zChar);
				return;
			}
		for(int alph = 1; alph < 3; alph ++)
			for(byte zChar = 6; zChar < 32; zChar ++)
				if(translateZCharToZSCII(zChar, alph) == zsciiChar)
				{
					//In V1+2, 2 & 3 are shift chars. In V3+, 4 & 5 are shift chars.
					target.accept((byte) (alph + (version < 3 ? 1 : 3)));
					target.accept(zChar);
					return;
				}
		target.accept((byte) (version < 3 ? 3 : 5));
		target.accept((byte) 6);
		target.accept((byte) (zsciiChar >>> 5));
		target.accept((byte) (zsciiChar & 31));
	}
}