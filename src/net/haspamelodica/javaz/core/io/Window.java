package net.haspamelodica.javaz.core.io;

public interface Window
{
	public void scroll(int y);
	public void showChar(CharacterDescription charDescr);
	public void newline();
	public void showPicture(int picture, int x, int y);
	public void erasePicture(int picture, int x, int y);
	public void eraseWindow();
	public WindowPropsAttrs getProperties();
}