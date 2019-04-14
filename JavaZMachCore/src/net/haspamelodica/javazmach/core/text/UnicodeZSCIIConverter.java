package net.haspamelodica.javazmach.core.text;

import net.haspamelodica.javazmach.GlobalConfig;

public class UnicodeZSCIIConverter
{
	private final boolean	dontIgnoreIllegalZSCIIChars;
	private final char[]	zscii13Equivalent;

	private boolean afterCR;

	public UnicodeZSCIIConverter(GlobalConfig config)
	{
		this.dontIgnoreIllegalZSCIIChars = config.getBool("text.zscii.dont_ignore_illegal_chars");
		boolean cr = config.getBool("text.unicode.newline.cr");
		boolean lf = config.getBool("text.unicode.newline.lf");
		this.zscii13Equivalent = cr
				? lf ? new char[] {'\r', '\n'} : new char[] {'\r'}
				: lf ? new char[] {'\n'} : System.lineSeparator().toCharArray();
	}

	public void zsciiToUnicode(int zsciiChar, UnicodeCharStreamReceiver target)
	{
		if(zsciiChar == 13)
		{
			for(int i = 0; i < zscii13Equivalent.length; i ++)
				target.accept(zscii13Equivalent[i]);
		} else
			target.accept(zsciiToUnicodeNoNL(zsciiChar));
	}
	public char zsciiToUnicodeNoNL(int zsciiChar)
	{
		if(zsciiChar == 9)
			return '\t';
		else if(zsciiChar > 31 && zsciiChar < 127)
			return (char) zsciiChar;
		else if(dontIgnoreIllegalZSCIIChars)
			if(zsciiChar == 13)
				throw new IllegalArgumentException("NL (ZSCII 13) given to zsciiToUnicodeNoNL");
			else
				throw new TextException("ZSCII char is undefined for output: " + zsciiChar);
		else
			return '?';
	}
	public void resetUnicodeToZSCII()
	{
		afterCR = false;
	}
	/**
	 * A return value of -1 means "no ZSCII char".
	 * This happens when NL consists of CRLF.
	 */
	public int unicodeToZSCII(char unicodeChar)
	{
		if(unicodeChar == '\r')
		{
			afterCR = true;
			return 13;
		} else if(afterCR && unicodeChar == '\n')
		{
			afterCR = false;
			return -1;
		} else
		{
			afterCR = false;
			if(unicodeChar == '\t')
				return 9;
			else if(unicodeChar > 31 && unicodeChar < 127)
				return unicodeChar;
			else
				return 0x3F;//question mark
		}
	}
}