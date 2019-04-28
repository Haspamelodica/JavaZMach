package net.haspamelodica.javazmach.core.text;

import java.util.Arrays;

public class FixedZSCIICharStream implements ZSCIICharStream
{
	private final int[] zsciiChars;

	public FixedZSCIICharStream(UnicodeZSCIIConverter unicodeConv, String str)
	{
		this(stringToZSCII(unicodeConv, str));
	}
	private static int[] stringToZSCII(UnicodeZSCIIConverter unicodeConv, String str)
	{
		int stringLength = str.length();
		int[] result = new int[stringLength];
		int zsciiLength = 0;
		for(int strI = 0; strI < stringLength; strI ++)
		{
			int zsciiChar = unicodeConv.unicodeToZSCII(str.charAt(strI));
			if(zsciiChar != -1)
				result[zsciiLength ++] = zsciiChar;
		}
		return Arrays.copyOf(result, zsciiLength);
	}
	public FixedZSCIICharStream(int[] zsciiChars)
	{
		this.zsciiChars = Arrays.copyOf(zsciiChars, zsciiChars.length);
	}

	@Override
	public void decode(ZSCIICharStreamReceiver target)
	{
		for(int i = 0; i < zsciiChars.length; i ++)
			target.accept(zsciiChars[i]);
	}
}