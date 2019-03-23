package net.haspamelodica.javaz;

import java.util.Arrays;

public class ZCharsUnpacker
{
	private final SequentialMemoryAccess mem;

	public ZCharsUnpacker(SequentialMemoryAccess mem)
	{
		this.mem = mem;
	}

	public void unpack(ZCharString target)
	{
		int zCharsWord;
		byte[] zChars = target.chars;
		int zCharIndex = 0;
		do
		{
			zCharsWord = mem.readNextWord();
			if(zChars.length < zCharIndex + 3)
				zChars = Arrays.copyOf(zChars, zCharIndex + 10);
			zChars[zCharIndex ++] = (byte) ((zCharsWord & 0x7C_00) >> 10);//bits 14-10
			zChars[zCharIndex ++] = (byte) ((zCharsWord & 0x03_E0) >> 5);//bits 9-5
			zChars[zCharIndex ++] = (byte) (zCharsWord & 0x00_1F);//bits 4-0
		} while((zCharsWord & 0x80_00) == 0);
		target.chars = zChars;
		target.length = zCharIndex;
	}
}