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

		this.undefinedHeaderFieldDynamic = !config.getBool("header.dont_allow_undefined_field_write");

		this.mem = mem;
	}

	public int getField(HeaderField field)
	{
		checkVersion(field);
		return getFieldUnchecked(this.mem, field);
	}
	/**
	 * user:
	 * 0 means game (Dyn),
	 * 1 means interpreter (Int),
	 * 2 means interpreter on reset (Rst);
	 * other means don't check.
	 */
	public void setField(HeaderField field, int val, int user)
	{
		checkVersion(field);
		if((user == 0 && !field.isDyn) || (user == 1 && !field.isInt) || (user == 2 && !field.isRst))
			throw new HeaderException("Field" + field + " is not dynamic (for " + (user == 0 ? "Dyn)" : user == 1 ? "Int)" : "Rst)"));
		setFieldUnchecked(this.mem, field, val);
	}
	//Maybe support multi-byte fields? (Compiler version...)
	private void checkVersion(HeaderField field)
	{
		if(version < field.minVersion || (field.maxVersion >= 0 && version > field.maxVersion))
			throw new HeaderException("Field " + field + " doesn't exist in V" + version);
	}

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
			throw new IllegalArgumentException("Field is neither byte nor word!");

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
		HeaderField field = getHeaderField(byteAddr);
		if(field == null)
			return undefinedHeaderFieldDynamic;
		if(field.isBitfield)
		{
			int oldVal = mem.readByte(byteAddr);
			int bitOff = (field.addr + field.len - 1 - byteAddr) << 3;
			for(int bit = 0; bit < 8; bit ++)
			{
				int bitMask = 1 << bit;
				if((val & bitMask) != (oldVal & bitMask))
				{
					HeaderField bitField = getHeaderField(field, bit + bitOff);
					if(bitField == null)
					{
						if(!undefinedHeaderFieldDynamic)
							return false;
					} else if(!bitField.isDyn)
						return false;
				}
			}
			return true;
		} else
			return field.isDyn;
	}

	private HeaderField getHeaderField(int byteAddr)
	{
		//TODO make this faster; also don't create objects!
		for(HeaderField f : HeaderField.values())
			if(f.bitfield == null && version >= f.minVersion && (version <= f.maxVersion || f.maxVersion < 0) && f.addr <= byteAddr && f.addr + f.len > byteAddr)
				return f;
		return null;
	}
	private HeaderField getHeaderField(HeaderField bitfield, int bitAddr)
	{
		//TODO make this faster; also don't create objects!
		for(HeaderField f : HeaderField.values())
			if(f.bitfield == bitfield && version >= f.minVersion && (version <= f.maxVersion || f.maxVersion < 0) && f.addr == bitAddr)
				return f;
		return null;
	}
}