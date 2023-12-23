package net.haspamelodica.javazmach.core.header;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.memory.ReadOnlyMemory;
import net.haspamelodica.javazmach.core.memory.WritableMemory;

public class HeaderParser
{
	private final int version;

	private final boolean undefinedHeaderFieldDynamic;

	private final WritableMemory mem;

	public HeaderParser(GlobalConfig config, int version, WritableMemory mem)
	{
		this.version = version;

		this.undefinedHeaderFieldDynamic = config.getBool("header.allow_undefined_field_write");

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

		int valueOrBitfield;
		if(len == 1)
			valueOrBitfield = mem.readByte(addr);
		else if(len == 2)
			valueOrBitfield = mem.readWord(addr);
		else if(len == 4)
			valueOrBitfield = mem.readInt(addr);
		else
			throw new IllegalArgumentException("Field is neither byte nor word!");

		if(isSingleBit)
			return (valueOrBitfield >>> field.addr) & 1;
		else
			return valueOrBitfield;
	}
	public static void setFieldUnchecked(WritableMemory mem, HeaderField field, int val)
	{
		boolean isSingleBit = field.len == 0;
		int addr = isSingleBit ? field.bitfield.addr : field.addr;
		int len = isSingleBit ? field.bitfield.len : field.len;

		if(len != 1 && len != 2 && len != 4)
			throw new IllegalArgumentException("Field neither byte nor word!");

		int valueOrBitfield;
		if(isSingleBit)
		{
			if(len == 1)
				valueOrBitfield = mem.readByte(addr);
			else
				valueOrBitfield = mem.readWord(addr);
			valueOrBitfield |= (val & 1) << field.addr;
		} else
			valueOrBitfield = val;

		if(len == 1)
			mem.writeByte(addr, valueOrBitfield);
		else if(len == 2)
			mem.writeWord(addr, valueOrBitfield);
		else
			mem.writeInt(addr, valueOrBitfield);
	}

	public static void getFieldUncheckedBytes(ReadOnlyMemory mem, HeaderField field, byte[] data, int off, int len)
	{
		if(field.len == 0)
			throw new IllegalArgumentException("Field is a single bit");
		if(len > field.len)
			throw new IllegalArgumentException("Length mismatch: expected at most " + field.len + ", but was " + len);
		mem.readNBytes(field.addr, data, off, len);
	}
	public static void getFieldUncheckedBytes(ReadOnlyMemory mem, HeaderField field, byte[] data)
	{
		if(field.len == 0)
			throw new IllegalArgumentException("Field is a single bit");
		if(data.length > field.len)
			throw new IllegalArgumentException("Length mismatch: expected at most " + field.len + ", but was " + data.length);
		mem.readNBytes(field.addr, data);
	}
	public static byte[] getFieldUncheckedBytes(ReadOnlyMemory mem, HeaderField field)
	{
		if(field.len == 0)
			throw new IllegalArgumentException("Field is a single bit");

		return mem.readNBytes(field.addr, field.len);
	}

	public static void setFieldUncheckedBytes(WritableMemory mem, HeaderField field, byte[] data, int off, int len)
	{
		if(field.len == 0)
			throw new IllegalArgumentException("Field is a single bit");
		if(len > field.len)
			throw new IllegalArgumentException("Length mismatch: expected at most " + field.len + ", but was " + len);
		mem.writeNBytes(field.addr, data, off, len);
	}
	public static void setFieldUncheckedBytes(WritableMemory mem, HeaderField field, byte[] data)
	{
		if(field.len == 0)
			throw new IllegalArgumentException("Field is a single bit");
		if(data.length > field.len)
			throw new IllegalArgumentException("Length mismatch: expected at most " + field.len + ", but was " + data.length);
		mem.writeNBytes(field.addr, data);
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