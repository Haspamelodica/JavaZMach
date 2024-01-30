package net.haspamelodica.javazmach.core.text;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.ReadOnlyMemory;

public class ZSCIICharZCharConverter
{
	private final int version;

	private final ZCharsAlphabetTable alphabetTable;

	public ZSCIICharZCharConverter(int version, ZCharsAlphabetTable alphabetTable)
	{
		this.version = version;
		this.alphabetTable = alphabetTable;
	}
	public ZSCIICharZCharConverter(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem)
	{
		this(version, new ZCharsAlphabetTableFromStoryfile(config, version, headerParser, mem));
	}
	public void reset()
	{
		alphabetTable.reset();
	}

	public int translateZCharToZSCII(byte zChar, int alphabet)
	{
		return alphabetTable.translateZCharToZSCII(zChar, alphabet);
	}
	public void translateZSCIIToZChars(int zsciiChar, ZCharStreamReceiver target)
	{
		translateZSCIIToZChars(zsciiChar, target, version, alphabetTable);
	}
	public static void translateZSCIIToZChars(int zsciiChar, ZCharStreamReceiver target, int version, ZCharsAlphabetTable alphabetTable)
	{
		if(zsciiChar == 32)
		{
			target.accept((byte) 0);
			return;
		}

		if(zsciiChar == 13)
		{
			if(version == 1)
				target.accept((byte) 1);
			else
			{
				target.accept((byte) (version < 3 ? 3 : 5));
				target.accept((byte) 7);
			}
			return;
		}

		//TODO make the following faster
		for(byte zChar = 6; zChar < 32; zChar ++)
			if(alphabetTable.translateZCharToZSCII(zChar, 0) == zsciiChar)
			{
				target.accept(zChar);
				return;
			}
		for(int alph = 1; alph < 3; alph ++)
			for(byte zChar = 6; zChar < 32; zChar ++)
				if(alphabetTable.translateZCharToZSCII(zChar, alph) == zsciiChar)
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