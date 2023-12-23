package net.haspamelodica.javazmach.core.text;

import net.haspamelodica.javazmach.GlobalConfig;

public class UnicodeZSCIIConverterNoSpecialChars implements UnicodeZSCIIConverter
{
	private final boolean	dontIgnoreIllegalZSCIIChars;
	private final char[]	zscii13Equivalent;

	private boolean afterCR;

	public UnicodeZSCIIConverterNoSpecialChars(GlobalConfig config)
	{
		this.dontIgnoreIllegalZSCIIChars = config.getBool("text.zscii.dont_ignore_illegal_chars");
		boolean cr = config.getBool("text.unicode.newline.cr");
		boolean lf = config.getBool("text.unicode.newline.lf");
		this.zscii13Equivalent = cr
				? lf ? new char[] {'\r', '\n'} : new char[] {'\r'}
				: lf ? new char[] {'\n'} : System.lineSeparator().toCharArray();
	}

	@Override
	public void zsciiToUnicode(int zsciiChar, UnicodeCharStreamReceiver target)
	{
		if(zsciiChar == 13)
		{
			for(int i = 0; i < zscii13Equivalent.length; i ++)
				target.accept(zscii13Equivalent[i]);
		} else
			target.accept(zsciiToUnicodeNoNL(zsciiChar));
	}
	@Override
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
	@Override
	public void resetUnicodeToZSCII()
	{
		afterCR = false;
	}
	@Override
	public int unicodeToZSCII(char unicodeChar)
	{
		if(unicodeChar == '\r')
		{
			afterCR = true;
			return 13;
		} else if(unicodeChar == '\n' && afterCR)
		{
			afterCR = false;
			return -1;
		} else
		{
			afterCR = false;
			return unicodeToZsciiNoCR(unicodeChar);
		}
	}

	public static int unicodeToZsciiNoCR(int unicodeCodepoint)
	{
		if(unicodeCodepoint == '\n')
			return 13;
		else if(unicodeCodepoint == '\t')
			return 9;
		else if(unicodeCodepoint > 31 && unicodeCodepoint < 127)
			return unicodeCodepoint;
		else
			return 0x3F;//question mark
	}
}