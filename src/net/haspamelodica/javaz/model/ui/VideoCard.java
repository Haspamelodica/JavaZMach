package net.haspamelodica.javaz.model.ui;

public interface VideoCard
{
	public void showStatusBar(int locationStartByteAddr, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame);
	public void scrollWindows(int window, int y);
	public void showChar(CharacterDescription charDescr);
	public void newline();
	public void showPicture(int picture, int x, int y);
	public void erasePicture(int picture, int x, int y);
	public void eraseWindow(int window);
	public void eraseScreen();

	public void flush();
}