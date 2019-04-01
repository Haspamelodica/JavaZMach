package net.haspamelodica.javaz.core.io;

public interface VideoCard
{
	public void eraseScreen();
	public void flushScreen();
	public int getScreenWidth();
	public int getScreenHeight();
	public void showStatusBar(int locationStartByteAddr, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame);
	public int getTrueColor(int color);
	public int getCharWidth(CharacterDescription charDescr);
	public Window createWindow();
}