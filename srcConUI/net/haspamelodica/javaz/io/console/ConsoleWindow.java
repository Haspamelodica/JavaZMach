package net.haspamelodica.javaz.io.console;

import java.io.IOException;

import net.haspamelodica.javaz.core.io.CharacterDescription;
import net.haspamelodica.javaz.core.io.Window;
import net.haspamelodica.javaz.core.io.WindowPropsAttrs;
import net.haspamelodica.javaz.core.memory.ReadOnlyByteSet;
import net.haspamelodica.javaz.core.memory.SequentialRWMemoryAccess;
import net.haspamelodica.javaz.core.text.UnicodeZSCIIConverter;

public class ConsoleWindow implements Window
{
	private final WindowPropsAttrs		properties	= new WindowPropsAttrs();
	private final UnicodeZSCIIConverter	unicodeConv;

	public ConsoleWindow(UnicodeZSCIIConverter unicodeConv)
	{
		this.unicodeConv = unicodeConv;
	}

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
	public int inputToTextBuffer(int maxZSCIIChars, SequentialRWMemoryAccess targetTextBuffer, ReadOnlyByteSet terminatingZSCIIChars)
	{
		unicodeConv.resetUnicodeToZSCII();
		for(int read = 0; read < maxZSCIIChars; read ++)
			try
			{
				int nextChar = System.in.read();
				if(nextChar == -1)
					return -2;
				int zsciiChar = unicodeConv.unicodeToZSCII((char) nextChar);
				if(zsciiChar != -1)
				{
					if(zsciiChar == 13 || terminatingZSCIIChars.contains(zsciiChar))
						return zsciiChar;
					targetTextBuffer.writeNextByte(zsciiChar);
				}
			} catch(IOException e)
			{
				return -3;
			}
		return -1;
	}
	@Override
	public WindowPropsAttrs getProperties()
	{
		return properties;
	}
}