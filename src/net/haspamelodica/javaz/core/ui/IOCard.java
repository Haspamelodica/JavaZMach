package net.haspamelodica.javaz.core.ui;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;

public class IOCard
{
	private final int version;

	private final HeaderParser	headerParser;
	private final VideoCard		videoCard;

	private final CharacterDescription charDescrBuf;

	public IOCard(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, VideoCardDefinition vCardDef)
	{
		this.version = version;

		this.headerParser = headerParser;
		this.videoCard = vCardDef.create(config, version, headerParser);

		this.charDescrBuf = new CharacterDescription();
	}

	public void reset()
	{
		//TODO
	}

	public void printZSCII(int zsciiChar)
	{
		charDescrBuf.unicodeCodepoint = (char) zsciiChar;
		videoCard.showChar(charDescrBuf);
		videoCard.flush();
	}
}