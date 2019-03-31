package net.haspamelodica.javaz.model.ui;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.model.HeaderParser;

public interface VideoCardDefinition
{
	public VideoCard create(GlobalConfig config, int version, HeaderParser headerParser);
}