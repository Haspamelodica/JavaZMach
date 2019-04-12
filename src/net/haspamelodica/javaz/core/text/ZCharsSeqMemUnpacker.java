package net.haspamelodica.javaz.core.text;

import net.haspamelodica.javaz.core.memory.SequentialMemoryAccess;

public class ZCharsSeqMemUnpacker implements ZCharStream
{
	private final SequentialMemoryAccess mem;

	public ZCharsSeqMemUnpacker(SequentialMemoryAccess mem)
	{
		this.mem = mem;
	}

	@Override
	public void decode(ZCharStreamReceiver target)
	{
		int zCharsWord;
		do
		{
			zCharsWord = mem.readNextWord();
			target.accept((byte) ((zCharsWord & 0x7C_00) >>> 0xA));
			target.accept((byte) ((zCharsWord & 0x03_E0) >>> 0x5));
			target.accept((byte) ((zCharsWord & 0x00_1F) >>> 0x0));
		} while((zCharsWord & 0x80_00) == 0);
	}
}