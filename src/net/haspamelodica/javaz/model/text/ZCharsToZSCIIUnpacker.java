package net.haspamelodica.javaz.model.text;

import static net.haspamelodica.javaz.model.HeaderParser.AbbrevTableLocLoc;
import static net.haspamelodica.javaz.model.HeaderParser.AlphabetTableLocLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.model.HeaderParser;
import net.haspamelodica.javaz.model.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.model.memory.SequentialMemoryAccess;

public class ZCharsToZSCIIUnpacker
{
	private static final int[]	defaultA0		= {};
	private static final int[]	defaultA1		= {};
	private static final int[]	defaultA2_V1	= {};
	private static final int[]	defaultA2		= {};

	private final int version;

	private final HeaderParser		headerParser;
	private final ReadOnlyMemory	mem;
	private final ZCharStreamSource	source;

	private final SequentialMemoryAccess	abbrevSeqMem;
	private final ZCharStreamSource			abbrevSource;

	private int	abbreviationsTableLoc;
	private int	alphabetTableLoc;

	public ZCharsToZSCIIUnpacker(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, ZCharStreamSource source)
	{
		this.version = version;

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

	public void decode(ZSCIICharStreamReceiver target)
	{
		decode(target, source);
	}
	private void decode(ZSCIICharStreamReceiver target, ZCharStreamSource source)
	{
		source.reset();
		do
		{
			byte zChar = source.nextZChar();
			switch(zChar)
			{
				//TODO
			}
		} while(source.hasNext());
	}
}