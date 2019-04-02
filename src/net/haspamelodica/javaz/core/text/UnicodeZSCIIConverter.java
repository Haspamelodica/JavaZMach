package net.haspamelodica.javaz.core.text;

import net.haspamelodica.javaz.GlobalConfig;

public class UnicodeZSCIIConverter
{
	private final boolean	dontIgnoreIllegalZSCIIChars;
	private final char[]	zscii13Equivalent;

	private boolean afterFirstNewlineChar;

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
			target.accept('\r');
			target.accept('\n');
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
			throw new TextException("ZSCII char is undefined for output: " + zsciiChar);
		else
			return '?';
	}
	public void resetUnicodeToZSCII()
	{
		afterFirstNewlineChar = false;
	}
	/**
	 * A return value of -1 means "no ZSCII char".
	 * This happens when NL consists of two chars.
	 */
	public int unicodeToZSCII(char unicodeChar)
	{
		if(unicodeChar == zscii13Equivalent[0])
		{
			afterFirstNewlineChar = true;
			return 13;
		} else if(afterFirstNewlineChar && zscii13Equivalent.length > 1 && unicodeChar == zscii13Equivalent[1])
		{
			afterFirstNewlineChar = false;
			return -1;
		} else
		{
			afterFirstNewlineChar = false;
			if(unicodeChar == '\t')
				return 9;
			else if(unicodeChar > 31 && unicodeChar < 127)
				return unicodeChar;
			else
				return 0x3F;//question mark
		}
	}
}