package net.haspamelodica.javaz.io.console;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.io.CharacterDescription;
import net.haspamelodica.javaz.core.io.VideoCard;
import net.haspamelodica.javaz.core.io.Window;
import net.haspamelodica.javaz.core.text.ZCharsToZSCIIConverter;

public class ConsoleVideoCard implements VideoCard
{
	public ConsoleVideoCard(GlobalConfig config, int version, HeaderParser headerParser)
	{}

	@Override
	public void eraseScreen()
	{}
	@Override
	public void flushScreen()
	{}
	@Override
	public int getScreenWidth()
	{
		return 0;
	}
	@Override
	public int getScreenHeight()
	{
		return 0;
	}
	@Override
	public void showStatusBar(ZCharsToZSCIIConverter locationConv, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame)
	{}

	@Override
	public int getTrueColor(int color)
	{
		return 0;
	}
	@Override
	public int getCharWidth(CharacterDescription charDescr)
	{
		return 0;
	}
	@Override
	public Window createWindow()
	{
		return new ConsoleWindow();
	}
}