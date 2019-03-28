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
	private int	objTreeLoc;

	public ObjectTree(GlobalConfig config, int version, HeaderParser headerParser, WritableMemory mem)
	{
		this.version = version;

		this.headerParser = headerParser;
		this.mem = mem;
	}
	public void reset()
	{
		propDefaultTableLoc = headerParser.getField(ObjTableLocLoc);
		objTreeLoc = propDefaultTableLoc + (version < 4 ? 62 : 126);//31 / 63 words
	}
}