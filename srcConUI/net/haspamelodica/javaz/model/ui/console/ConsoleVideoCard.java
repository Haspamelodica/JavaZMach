package net.haspamelodica.javaz.model.ui.console;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.model.HeaderParser;
import net.haspamelodica.javaz.model.ui.CharacterDescription;
import net.haspamelodica.javaz.model.ui.VideoCard;

public class ConsoleVideoCard implements VideoCard
{
	public ConsoleVideoCard(GlobalConfig config, int version, HeaderParser headerParser)
	{}
	@Override
	public void showStatusBar(int locationStartByteAddr, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame)
	{}
	@Override
	public void scrollWindows(int window, int y)
	{}
	@Override
	public void showChar(CharacterDescription charDescr)
	{
		System.out.print(charDescr.unicodeCodepoint);
	}
	@Override
	public void newline()
	{
		System.out.println();
	}
	@Override
	public void showPicture(int picture, int x, int y)
	{}
	@Override
	public void erasePicture(int picture, int x, int y)
	{}
	@Override
	public void eraseWindow(int window)
	{}
	@Override
	public void eraseScreen()
	{}
	@Override
	public void flush()
	{}
}