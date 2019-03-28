package net.haspamelodica.javaz.model.objects;

import static net.haspamelodica.javaz.model.HeaderParser.ObjTableLocLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.model.HeaderParser;
import net.haspamelodica.javaz.model.memory.WritableMemory;

public class ObjectTree
{
	private final int version;

	private final HeaderParser		headerParser;
	private final WritableMemory	mem;

	private int	propDefaultTableLoc;
	private int	objectsOffset;

	public ObjectTree(GlobalConfig config, int version, HeaderParser headerParser, WritableMemory mem)
	{
		this.version = version;

		this.headerParser = headerParser;
		this.mem = mem;
	}
	public void reset()
	{
		propDefaultTableLoc = headerParser.getField(ObjTableLocLoc);
		//31 / 63 words (property defaults table) minus 6 / 14 bytes (object tree starts with object number 1)
		objectsOffset = propDefaultTableLoc + (version < 4 ? 53 : 112);
	}
	/**
	 * Returns 1 if the given attribute is set, 0 if clear.
	 */
	public int getAttribute(int objNumber, int attribute)
	{
		int objAddress = getObjAddress(objNumber);
		int attributeBit = ~attribute & 0x7;//equal to 7 - (attribute & 0x7)
		int attributeByte = attribute >> 3;

		return (mem.readByte(objAddress + attributeByte) >> attributeBit) & 0b1;
	}
	public void setAttribute(int objNumber, int attribute, int val)
	{
		int objAddress = getObjAddress(objNumber);
		int attributeBit = ~attribute & 0x7;//equal to 7 - (attribute & 0x7)
		int attributeByte = attribute >> 3;

		int attributeBitMask = val << attributeBit;
		int oldVal = mem.readByte(objAddress + attributeByte);
		mem.writeByte(objAddress + attributeByte, (oldVal & ~attributeBitMask) | attributeBitMask);
	}

	public int getParent(int objNumber)
	{
		int objAddress = getObjAddress(objNumber);
		return version < 4 ? mem.readByte(objAddress + 4) : mem.readWord(objAddress + 6);
	}
	public void setParent(int objNumber, int parent)
	{
		int objAddress = getObjAddress(objNumber);
		if(version < 4)
			mem.writeByte(objAddress + 4, parent);
		else
			mem.writeWord(objAddress + 6, parent);
	}
	public int getSibling(int objNumber)
	{
		int objAddress = getObjAddress(objNumber);
		return version < 4 ? mem.readByte(objAddress + 5) : mem.readWord(objAddress + 8);
	}
	public void setSibling(int objNumber, int sibling)
	{
		int objAddress = getObjAddress(objNumber);
		if(version < 4)
			mem.writeByte(objAddress + 5, sibling);
		else
			mem.writeWord(objAddress + 8, sibling);
	}
	public int getChild(int objNumber)
	{
		int objAddress = getObjAddress(objNumber);
		return version < 4 ? mem.readByte(objAddress + 6) : mem.readWord(objAddress + 10);
	}
	public void setChild(int objNumber, int child)
	{
		int objAddress = getObjAddress(objNumber);
		if(version < 4)
			mem.writeByte(objAddress + 6, child);
		else
			mem.writeWord(objAddress + 10, child);
	}

	private int getObjAddress(int objNumber)
	{
		return objNumber * (version < 4 ? 9 : 14) + objectsOffset;
	}
}