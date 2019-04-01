package net.haspamelodica.javaz.core.objects;

import static net.haspamelodica.javaz.core.HeaderParser.ObjTableLocLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.memory.WritableMemory;

public class ObjectTree
{
	private final int version;

	private final boolean	checkPropsDescending;
	private final boolean	checkNoProp0;
	private final boolean	checkPutPropVal;

	private final HeaderParser		headerParser;
	private final WritableMemory	mem;

	private final int propNumberMask;

	private int	propDefaultsOffset;
	private int	objectsOffset;

	public ObjectTree(GlobalConfig config, int version, HeaderParser headerParser, WritableMemory mem)
	{
		this.version = version;

		this.checkPropsDescending = config.getBool("objects.properties.check_descending_order");
		this.checkNoProp0 = config.getBool("objects.properties.check_no_prop_0");
		this.checkPutPropVal = config.getBool("objects.properties.check_put_prop_val");

		this.headerParser = headerParser;
		this.mem = mem;

		this.propNumberMask = version > 3 ? 0x3F : 0x1F;
	}
	public void reset()
	{
		int propDefaultTableLoc = headerParser.getField(ObjTableLocLoc);
		propDefaultsOffset = propDefaultTableLoc - 2;
		//31 / 63 words (property defaults table) minus 6 / 14 bytes (object tree starts with object number 1)
		objectsOffset = propDefaultTableLoc + (version < 4 ? 53 : 112);
	}

	/**
	 * Returns 1 if the given attribute is set, 0 if clear.
	 */
	public int getAttribute(int objNumber, int attribute)
	{
		if(attribute < 0 || attribute > (version < 4 ? 31 : 47))
			throw new ObjectException("Illegal attribute");
		int objAddress = getObjAddress(objNumber);
		int attributeBit = ~attribute & 0x7;//equal to 7 - (attribute & 0x7)
		int attributeByte = attribute >>> 3;

		return (mem.readByte(objAddress + attributeByte) >>> attributeBit) & 0b1;
	}
	public void setAttribute(int objNumber, int attribute, int val)
	{
		int objAddress = getObjAddress(objNumber);
		int attributeBit = ~attribute & 0x7;//equal to 7 - (attribute & 0x7)
		int attributeByte = attribute >>> 3;

		int oldVal = mem.readByte(objAddress + attributeByte);
		mem.writeByte(objAddress + attributeByte, (oldVal & ~(1 << attributeBit)) | val << attributeBit);
	}

	public void insertObj(int objChild, int objNewParent)
	{
		removeObj(objChild);
		int objOldFirstChild = getChild(objNewParent);
		setChild(objNewParent, objChild);
		setParent(objChild, objNewParent);
		setSibling(objChild, objOldFirstChild);
	}
	public void removeObj(int objNumber)
	{
		int oldParent = getParent(objNumber);
		if(oldParent == 0)
			return;
		int sibling = getSibling(objNumber);
		if(getChild(oldParent) == objNumber)
			setChild(oldParent, sibling);
		else
			setSibling(getPreviousSibling(objNumber), sibling);
		setParent(objNumber, 0);
	}
	private int getPreviousSibling(int objNumber)
	{
		int sibling = getChild(getParent(objNumber));
		int previousSibling = 0;
		while(sibling != objNumber)
		{
			previousSibling = sibling;
			sibling = getSibling(sibling);
		}
		return previousSibling;
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

	public int getObjectNameLoc(int objNumber)
	{
		return getPropertiesTableLoc(objNumber) + 1;
	}
	public int getObjectNameWords(int objNumber)
	{
		return mem.readByte(getPropertiesTableLoc(objNumber));
	}
	public int getPropOrThrow(int objNumber, int propNumber)
	{
		int propAddr = getPropAddrOrThrow(objNumber, propNumber);
		int propSize = getPropSizeByPropAddr(propAddr);
		return propSize == 1 ? mem.readByte(propAddr) : mem.readWord(propAddr);
	}
	public int getPropOrDefault(int objNumber, int propNumber)
	{
		int propAddr = getPropAddr(objNumber, propNumber);
		if(propAddr == -1)
			return getPropDefault(propNumber);
		int propSize = getPropSizeByPropAddr(propAddr);
		return propSize == 1 ? mem.readByte(propAddr) : mem.readWord(propAddr);
	}
	public void putPropOrThrow(int objNumber, int propNumber, int val)
	{
		int propAddr = getPropAddrOrThrow(objNumber, propNumber);
		int propSize = getPropSizeByPropAddr(propAddr);
		if(propSize == 1)
			if((val & ~0xFF) != 0 && checkPutPropVal)
				throw new ObjectException("Value too high to fit into a property of length 1");
			else
				mem.writeByte(propAddr, val);
		else if((val & ~0xFFFF) != 0 && checkPutPropVal)
			throw new ObjectException("Value too high to fit into a property of length 2");
		else
			mem.writeWord(propAddr, val);
	}
	public int getNextProp(int objNumber, int propNumber)
	{
		int propSizeAddr;
		if(propNumber == 0)
			propSizeAddr = getFirstPropSizeAddr(objNumber);
		else
			propSizeAddr = getPropSizeAddrOrThrow(objNumber, propNumber);
		if(propSizeAddr == -1)
			return 0;
		return getPropNumberByPropSizeAddr(getNextPropSizeAddrByPropAddr(getPropAddrByPropSizeAddr(propSizeAddr)));
	}
	public int getPropAddrOrThrow(int objNumber, int propNumber)
	{
		return getPropAddrByPropSizeAddr(getPropSizeAddrOrThrow(objNumber, propNumber));
	}
	public int getPropAddr(int objNumber, int propNumber)
	{
		int propSizeAddr = getPropSizeAddr(objNumber, propNumber);
		if(propSizeAddr == -1)
			return -1;
		return getPropAddrByPropSizeAddr(propSizeAddr);
	}
	public int getNextPropAddr(int propAddr)
	{
		int nextPropSizeAddr = getNextPropSizeAddrByPropAddr(propAddr);
		if(nextPropSizeAddr == -1)
			return -1;
		return getPropAddrByPropSizeAddr(nextPropSizeAddr);
	}
	public int getPropNumber(int propAddr)
	{
		return getPropNumberByPropSizeAddr(getPropSizeAddrByPropAddr(propAddr));
	}
	public int getPropSize(int propAddr)
	{
		return getPropSizeByPropAddr(propAddr);
	}
	public int getFirstPropAddr(int objNumber)
	{
		int firstPropSizeAddr = getFirstPropSizeAddr(objNumber);
		if(mem.readByte(firstPropSizeAddr) == 0)
			return -1;
		return getPropAddrByPropSizeAddr(firstPropSizeAddr);
	}

	private int getPropSizeAddrOrThrow(int objNumber, int propNumber)
	{
		int propSizeAddr = getPropSizeAddr(objNumber, propNumber);
		if(propSizeAddr == -1)
			throw new ObjectException("Property not found");
		return propSizeAddr;
	}
	private int getPropSizeAddr(int objNumber, int propNumber)
	{
		if(propNumber == 0 || (propNumber & ~propNumberMask) != 0)
			throw new IllegalArgumentException("Invalid property number: " + propNumber);
		int currentPropSizeAddr = getFirstPropSizeAddr(objNumber);
		int lastPropNumber = propNumberMask + 1;
		while(true)
		{
			int currentFirstSizeByte = mem.readByte(currentPropSizeAddr);
			int currentPropNumber = getPropNumberByFirstSizeByte(currentFirstSizeByte);
			if(currentPropNumber == propNumber)
				return currentPropSizeAddr;
			if(checkPropsDescending && currentPropNumber < propNumber)
				return -1;
			if(currentPropNumber == 0 && checkNoProp0)
				throw new ObjectException("Property nubmer 0 illegal");
			if(lastPropNumber <= currentPropNumber && checkPropsDescending)
				throw new ObjectException("Properties not in descending order");
			lastPropNumber = currentPropNumber;
			int currentPropAddr = currentPropSizeAddr + getPropAddrPropSizeAddrDeltaBySizeByte(currentFirstSizeByte);
			currentPropSizeAddr = getNextPropSizeAddrByPropAddr(currentPropAddr);
			if(currentPropSizeAddr == -1)
				return -1;
		}
	}
	private int getNextPropSizeAddrByPropAddr(int propAddr)
	{
		int nextPropSizeAddr = propAddr + getPropSizeByPropAddr(propAddr);
		if(mem.readByte(nextPropSizeAddr) == 0)
			return -1;
		return nextPropSizeAddr;
	}
	private int getPropNumberByPropSizeAddr(int propSizeAddr)
	{
		return getPropNumberByFirstSizeByte(mem.readByte(propSizeAddr));
	}
	private int getPropSizeByPropAddr(int propAddr)
	{
		return getPropSizeByLastSizeByte(mem.readByte(propAddr - 1));
	}
	private int getPropNumberByFirstSizeByte(int firstPropSizeByte)
	{
		return firstPropSizeByte & propNumberMask;
	}
	private int getPropSizeByLastSizeByte(int lastPropSizeByte)
	{
		if(version > 3)
			if((lastPropSizeByte & 0x80) == 0)//bit 7
				return ((lastPropSizeByte & 0x40) >>> 6) + 1;//bit 6
			else
				//"In the second byte, bits 0 to 5 contain the property data length"
				//"A value of 0 as property data length (in the second byte) should be interpreted as a length of 64"
				return ((lastPropSizeByte - 1) & 0x3F) + 1;//bits 5-0
		else
			return ((lastPropSizeByte & 0xE0) >>> 5) + 1;//bits 7-5
	}
	private int getFirstPropSizeAddr(int objNumber)
	{
		int propertiesTableLoc = getPropertiesTableLoc(objNumber);
		return propertiesTableLoc + (mem.readByte(propertiesTableLoc) << 1) + 1;
	}
	private int getPropSizeAddrByPropAddr(int propAddr)
	{
		return propAddr - getPropAddrPropSizeAddrDeltaBySizeByte(mem.readByte(propAddr - 1));
	}
	private int getPropAddrByPropSizeAddr(int propSizeAddr)
	{
		return propSizeAddr + getPropAddrPropSizeAddrDeltaBySizeByte(mem.readByte(propSizeAddr));
	}
	private int getPropAddrPropSizeAddrDeltaBySizeByte(int sizeByte)
	{
		return version > 3 && (sizeByte & 0x80) != 0 ? 2 : 1;
	}
	public int getPropDefault(int propNumber)
	{
		return mem.readWord(propDefaultsOffset + (propNumber << 1));
	}

	private int getPropertiesTableLoc(int objNumber)
	{
		return mem.readWord(getObjAddress(objNumber) + (version < 4 ? 7 : 12));
	}
	private int getObjAddress(int objNumber)
	{
		if(objNumber < 1 || objNumber > 255)
			throw new ObjectException("Illegal object number: " + objNumber);
		return objNumber * (version < 4 ? 9 : 14) + objectsOffset;
	}
}