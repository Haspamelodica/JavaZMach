package net.haspamelodica.javaz.io.swt;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.io.VideoCard;
import net.haspamelodica.javaz.core.text.ZSCIICharStream;

public class SWTVideoCard implements VideoCard
{
	public SWTVideoCard(GlobalConfig config)
	{
		// TODO Auto-generated constructor stub
	}
	@Override
	public int getScreenWidth()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getScreenHeight()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getDefaultTrueFG()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getDefaultTrueBG()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getTrueColor(int color)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getCharWidth(int zsciiChar, int font, int style)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getFontHeight(int font)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void eraseArea(int x, int y, int w, int h, int trueBG)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void scroll(int y)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void showStatusBar(ZSCIICharStream location, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void showChar(int zsciiChar, int font, int style, int trueFB, int trueBG, int x, int y)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void showPicture(int picture, int x, int y)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void erasePicture(int picture, int x, int y, int trueBG)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void flushScreen()
	{
		// TODO Auto-generated method stub

	}
	@Override
	public int nextInputChar()
	{
		// TODO Auto-generated method stub
		return 0;
	}
}