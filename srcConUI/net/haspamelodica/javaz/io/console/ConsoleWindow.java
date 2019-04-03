package net.haspamelodica.javaz.io.console;

import net.haspamelodica.javaz.core.io.CharacterDescription;
import net.haspamelodica.javaz.core.io.IOException;
import net.haspamelodica.javaz.core.io.Window;
import net.haspamelodica.javaz.core.io.WindowPropsAttrs;
import net.haspamelodica.javaz.core.memory.ReadOnlyByteSet;
import net.haspamelodica.javaz.core.memory.WritableBuffer;
import net.haspamelodica.javaz.core.text.UnicodeZSCIIConverter;

public class ConsoleWindow implements Window
{
	private final WindowPropsAttrs		properties;
	private final UnicodeZSCIIConverter	unicodeConv;

	public ConsoleWindow(UnicodeZSCIIConverter unicodeConv)
	{
		this.properties = new WindowPropsAttrs();
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
	public int inputToTextBuffer(WritableBuffer targetTextBuffer, ReadOnlyByteSet terminatingZSCIIChars)
	{
		int maxZSCIIChars = targetTextBuffer.getCapacity();
		unicodeConv.resetUnicodeToZSCII();
		for(int read = 0; read < maxZSCIIChars; read ++)
			try
			{
				int nextChar = System.in.read();
				if(nextChar == -1)
					throw new IOException("EOF");
				int zsciiChar = unicodeConv.unicodeToZSCII((char) nextChar);
				//Range 'A'-'Z'
				if(zsciiChar > 0x40 && zsciiChar < 0x5B)
					//convert to lower case
					zsciiChar += 0x20;
				if(zsciiChar != -1)
				{
					if(zsciiChar == 13 || terminatingZSCIIChars.contains(zsciiChar))
						return zsciiChar;
					targetTextBuffer.writeNextEntryByte(0, zsciiChar);
					targetTextBuffer.finishEntry();
				}
			} catch(java.io.IOException e)
			{
				throw new IOException(e);
			}
		return -1;
	}
	@Override
	public WindowPropsAttrs getProperties()
	{
		return properties;
	}
}