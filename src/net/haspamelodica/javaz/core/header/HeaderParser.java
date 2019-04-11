package net.haspamelodica.javaz.core.header;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.WritableMemory;

public class HeaderParser
{
	private final int version;

	private final boolean undefinedHeaderFieldDynamic;

	private final WritableMemory mem;

	public HeaderParser(GlobalConfig config, int version, WritableMemory mem)
	{
		this.version = version;

		this.undefinedHeaderFieldDynamic = config.getBool("header.dont_allow_undefined_field_write");

		this.mem = mem;
	}

	//TODO check version
	public int getField(HeaderField field)
	{
		return getFieldUnchecked(this.mem, field);
	}
	//TODO enforce header write rules
	public void setField(HeaderField field, int val)
	{
		setFieldUnchecked(this.mem, field, val);
	}
	//Maybe support multi-byte fields? (Compiler version...)

	public static int getFieldUnchecked(ReadOnlyMemory mem, HeaderField field)
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
	public static void setFieldUnchecked(WritableMemory mem, HeaderField field, int val)
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
	public boolean isAllowedAsDynamicWrite(int byteAddr, int val)
	{
		if(byteAddr > 0x3F)
			return true;
		for(HeaderField f : HeaderField.values())
			if(f.addr <= byteAddr && f.addr + f.len > byteAddr)
				if(f.isBitfield)
					;
				else
					return f.isDyn;
		return undefinedHeaderFieldDynamic;
	}
}