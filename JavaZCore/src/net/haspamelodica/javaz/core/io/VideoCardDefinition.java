package net.haspamelodica.javaz.core.io;

import net.haspamelodica.javaz.GlobalConfig;

public interface VideoCardDefinition
{
	public VideoCard create(GlobalConfig config);
}