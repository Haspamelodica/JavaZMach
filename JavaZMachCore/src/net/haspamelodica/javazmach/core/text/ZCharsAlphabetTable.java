package net.haspamelodica.javazmach.core.text;

public interface ZCharsAlphabetTable
{
	public void reset();

	public int translateZCharToZSCII(byte zChar, int alphabet);
}
