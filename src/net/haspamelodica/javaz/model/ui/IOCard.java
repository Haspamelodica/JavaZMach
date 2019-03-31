package net.haspamelodica.javaz.model.ui;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.model.HeaderParser;
import net.haspamelodica.javaz.model.memory.ReadOnlyMemory;

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

	public void printZSCII(int zsciiChar)
	{
		charDescrBuf.unicodeCodepoint = (char) zsciiChar;
		videoCard.showChar(charDescrBuf);
		videoCard.flush();
	}
}