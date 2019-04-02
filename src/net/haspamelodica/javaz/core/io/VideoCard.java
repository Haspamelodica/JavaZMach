package net.haspamelodica.javaz.core.io;

import net.haspamelodica.javaz.core.text.ZCharsToZSCIIConverter;

public interface VideoCard
{
	public void eraseScreen();
	public void flushScreen();
	public int getScreenWidth();
	public int getScreenHeight();
	public void showStatusBar(ZCharsToZSCIIConverter locationConv, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame);
	public int getTrueColor(int color);
	public int getCharWidth(CharacterDescription charDescr);
	public Window createWindow();
}