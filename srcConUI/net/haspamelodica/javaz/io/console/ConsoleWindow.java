package net.haspamelodica.javaz.io.console;

import net.haspamelodica.javaz.core.io.CharacterDescription;
import net.haspamelodica.javaz.core.io.Window;
import net.haspamelodica.javaz.core.io.WindowPropsAttrs;

public class ConsoleWindow implements Window
{
	private final WindowPropsAttrs properties = new WindowPropsAttrs();

	@Override
	public void scroll(int y)
	{}
	@Override
	public void showChar(CharacterDescription charDescr)
	{
		System.out.print(charDescr.unicodeChar);
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
	public void eraseWindow()
	{}
	@Override
	public WindowPropsAttrs getProperties()
	{
		return properties;
	}
}