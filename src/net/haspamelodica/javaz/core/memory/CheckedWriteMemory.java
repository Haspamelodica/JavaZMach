package net.haspamelodica.javaz.core.memory;

import static net.haspamelodica.javaz.core.HeaderParser.StaticMemBaseLoc;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;

public class CheckedWriteMemory implements WritableMemory
{
	private final boolean	dontIgnoreStaticMemWrite;
	private final boolean	checkHeaderWrite;

	private final HeaderParser		headerParser;
	private final WritableMemory	mem;

	private int lastDynamicMemByte;

	public CheckedWriteMemory(GlobalConfig config, HeaderParser headerParser, WritableMemory mem)
	{
		this.dontIgnoreStaticMemWrite = config.getBool("memory.dont_ignore_static_mem_write");
		this.checkHeaderWrite = config.getBool("memory.check_header_write");

		this.headerParser = headerParser;
		this.mem = mem;
	}
	public void reset()
	{
		lastDynamicMemByte = headerParser.getField(StaticMemBaseLoc) - 1;
	}

	@Override
	public int getSize()
	{
		return mem.getSize();
	}
	@Override
	public int readByte(int byteAddr)
	{
		return mem.readByte(byteAddr);
	}
	@Override
	public int readWord(int byteAddr)
	{
		return mem.readWord(byteAddr);
	}
	@Override
	public void writeByte(int byteAddr, int val)
	{
		checkWrite(byteAddr);
		mem.writeByte(byteAddr, val);
	}
	@Override
	public void writeWord(int byteAddr, int val)
	{
		checkWrite(byteAddr);
		mem.writeWord(byteAddr, val);
	}
	private void checkWrite(int byteAddr)
	{
		if(dontIgnoreStaticMemWrite && byteAddr > lastDynamicMemByte)
			throw new MemoryException("Write to static / high memory");
		if(checkHeaderWrite && !headerParser.isDynamic(byteAddr))
			throw new MemoryException("Write to non-dynamic header field");
	}
}