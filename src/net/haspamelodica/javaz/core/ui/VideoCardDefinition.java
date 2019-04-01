package net.haspamelodica.javaz.core.ui;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;

public interface VideoCardDefinition
{
	public VideoCard create(GlobalConfig config, int version, HeaderParser headerParser);
}