package net.haspamelodica.javazmach.io.console;

import java.io.InputStreamReader;
import java.io.Reader;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.io.IOException;
import net.haspamelodica.javazmach.core.io.VideoCard;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverter;
import net.haspamelodica.javazmach.core.text.ZSCIICharStream;
import net.haspamelodica.javazmach.core.text.ZSCIIToUnicodeConverterStream;

public class ConsoleVideoCard implements VideoCard
{
	private final Reader						in;
	private final UnicodeZSCIIConverter			unicodeConv;
	private final ZSCIIToUnicodeConverterStream	unicodeConvStream;

	private int lastY = -1;

	public ConsoleVideoCard(GlobalConfig config)
	{
		this.in = new InputStreamReader(System.in);
		this.unicodeConv = new UnicodeZSCIIConverter(config);
		this.unicodeConvStream = new ZSCIIToUnicodeConverterStream(unicodeConv);
	}

	@Override
	public void scroll(int y)
	{
		if(lastY != -1)
			lastY -= y;
	}
	@Override
	public void showChar(int zsciiChar, int font, int style, int trueFB, int trueBG, int x, int y)
	{
		if(lastY == -1)
			lastY = y;
		else if(lastY > y)
		{
			System.out.println();
			System.out.println("<old lines get overwritten - cannot simulate this in console>");
			lastY = y;
		} else
			for(; lastY < y; lastY ++)
				System.out.println();
		char unicodeChar = unicodeConv.zsciiToUnicodeNoNL(zsciiChar);
		if(unicodeChar != -1)
			System.out.print(unicodeChar);
	}

	@Override
	public void flushScreen()
	{}
	@Override
	public int getScreenWidth()
	{
		return 80;
	}
	@Override
	public int getScreenHeight()
	{
		return 10;
	}
	@Override
	public void showStatusBar(ZSCIICharStream location, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame)
	{
		System.out.println();
		System.out.print("Status line: ");
		unicodeConvStream.reset(location);
		unicodeConvStream.decode(System.out::print);
		System.out.print(" | ");
		System.out.print(scoreOrHours);
		System.out.print(isTimeGame ? ":" : " | ");
		System.out.print(turnsOrMinutes);
		System.out.println();
	}
	@Override
	public int getTrueColor(int color)
	{
		return 0;
	}
	@Override
	public int nextInputChar()
	{
		try
		{
			int unicodeChar;
			do
				unicodeChar = unicodeConv.unicodeToZSCII((char) in.read());
			while(unicodeChar == -1);
			return unicodeChar;
		} catch(java.io.IOException e)
		{
			throw new IOException(e);
		}
	}
	@Override
	public int getDefaultTrueFG()
	{
		return 0;
	}
	@Override
	public int getDefaultTrueBG()
	{
		return 0;
	}
	@Override
	public void eraseArea(int x, int y, int w, int h, int trueBG)
	{}
	@Override
	public void showPicture(int picture, int x, int y)
	{}
	@Override
	public void erasePicture(int picture, int x, int y, int trueBG)
	{}
	@Override
	public int getCharWidth(int zsciiChar, int font, int style)
	{
		return 1;
	}
	@Override
	public int getFontHeight(int font)
	{
		return 1;
	}
}