package net.haspamelodica.javazmach.core.text;

import static net.haspamelodica.javazmach.core.header.HeaderField.AlphabetTableLoc;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.ReadOnlyMemory;

public class ZCharsAlphabetTableFromStoryfile implements ZCharsAlphabetTable
{
	private final int version;

	private final boolean ignoreZSCIINotInRange;

	private final HeaderParser		headerParser;
	private final ReadOnlyMemory	mem;

	private final ZCharsAlphabetTable alphabetTableDefault;

	private int alphabetTableLoc;

	public ZCharsAlphabetTableFromStoryfile(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem)
	{
		this.version = version;

		this.ignoreZSCIINotInRange = config.getBool("text.zscii.ignore_zscii_not_in_range");

		this.headerParser = headerParser;
		this.mem = mem;

		this.alphabetTableDefault = new ZCharsAlphabetTableDefault(version);
	}
	@Override
	public void reset()
	{
		if(version > 4)
			alphabetTableLoc = headerParser.getField(AlphabetTableLoc);
	}

	@Override
	public int translateZCharToZSCII(byte zChar, int alphabet)
	{
		if(alphabetTableLoc == 0)
			return alphabetTableDefault.translateZCharToZSCII(zChar, alphabet);

		int zscii = mem.readWord(alphabetTableLoc + ((zChar - 6) << 1) + (alphabet * 26));
		if(zscii > 0x3FF && !ignoreZSCIINotInRange)
			throw new TextException("Out-of-range ZSCII char: " + zscii);
		return zscii;
	}
}