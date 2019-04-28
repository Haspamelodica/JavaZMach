package net.haspamelodica.javazmach.core.text;

public interface UnicodeZSCIIConverter
{
	public void zsciiToUnicode(int zsciiChar, UnicodeCharStreamReceiver target);
	public char zsciiToUnicodeNoNL(int zsciiChar);
	public void resetUnicodeToZSCII();
	/**
	 * A return value of -1 means "no ZSCII char".
	 * This happens when NL consists of CRLF.
	 */
	public int unicodeToZSCII(char unicodeChar);
}