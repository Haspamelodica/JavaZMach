package net.haspamelodica.javaz.core.io;

import net.haspamelodica.javaz.core.text.ZSCIICharStream;

public interface VideoCard
{
	public int getScreenWidth();
	public int getScreenHeight();
	public int getDefaultTrueFG();
	public int getDefaultTrueBG();
	public int getTrueColor(int color);
	public int getCharWidth(int zsciiChar, int font, int style);
	public int getFontHeight(int font);
	public void eraseArea(int x, int y, int w, int h, int trueBG);
	public void scroll(int y);
	public void showStatusBar(ZSCIICharStream location, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame);
	public void showChar(int zsciiChar, int font, int style, int trueFB, int trueBG, int x, int y);
	public void showPicture(int picture, int x, int y);
	public void erasePicture(int picture, int x, int y, int trueBG);
	public void flushScreen();
	public default void hintInputCharUsage(InputUsageHint hint)
	{}
	public int nextInputChar();//TODO move to another class
}