package net.haspamelodica.javaz.core.io;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.text.UnicodeZSCIIConverter;

public interface VideoCardDefinition
{
	public VideoCard create(GlobalConfig config, int version, HeaderParser headerParser, UnicodeZSCIIConverter unicodeConv);
}