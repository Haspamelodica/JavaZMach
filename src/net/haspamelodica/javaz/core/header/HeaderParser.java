package net.haspamelodica.javaz.core.header;

import net.haspamelodica.javaz.core.memory.WritableMemory;

public class HeaderParser
{
	private final WritableMemory mem;

	public HeaderParser(WritableMemory memory)
	{
		this.mem = memory;
	}

	//TODO check version?
	public int getField(HeaderField field)
	{
		boolean isSingleBit = field.len == 0;
		int addr = isSingleBit ? field.bitfield.addr : field.addr;
		int len = isSingleBit ? field.bitfield.len : field.len;

		int byteOrWord;
		if(len == 1)
			byteOrWord = mem.readByte(addr);
		else if(len == 2)
			byteOrWord = mem.readWord(addr);
		else
			throw new IllegalArgumentException("Field neither byte nor word!");

		if(isSingleBit)
			return (byteOrWord >>> field.addr) & 1;
		else
			return byteOrWord;
	}
	public void setField(HeaderField field, int val)
	{
		boolean isSingleBit = field.len == 0;
		int addr = isSingleBit ? field.bitfield.addr : field.addr;
		int len = isSingleBit ? field.bitfield.len : field.len;

		if(len != 1 && len != 2)
			throw new IllegalArgumentException("Field neither byte nor word!");

		int byteOrWord;
		if(isSingleBit)
		{
			if(len == 1)
				byteOrWord = mem.readByte(addr);
			else
				byteOrWord = mem.readWord(addr);
			byteOrWord |= (val & 1) << field.addr;
		} else
			byteOrWord = val;

		if(len == 1)
			mem.writeByte(addr, byteOrWord);
		else
			mem.writeWord(addr, byteOrWord);
	}
	//Maybe support multi-byte fields? (Compiler version...)
	public boolean isDynamic(int byteAddr)
	{
		//TODO don't just disallow all header writes!
		return byteAddr > 0x3F;
	}
}