package net.haspamelodica.javazmach.core.memory;

import static net.haspamelodica.javazmach.core.header.HeaderField.StaticMemBase;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.header.HeaderParser;

public class CheckedWriteMemory implements WritableMemory
{
	private final boolean	ignoreStaticMemWrite;
	private final boolean	ignoreNondynamicHeaderWrite;

	private final HeaderParser		headerParser;
	private final WritableMemory	mem;

	private int lastDynamicMemByte;

	public CheckedWriteMemory(GlobalConfig config, HeaderParser headerParser, WritableMemory mem)
	{
		this.ignoreStaticMemWrite = config.getBool("memory.ignore_static_mem_write");
		this.ignoreNondynamicHeaderWrite = config.getBool("memory.ignore_nondynamic_header_write");

		this.headerParser = headerParser;
		this.mem = mem;
	}
	public void reset()
	{
		lastDynamicMemByte = headerParser.getField(StaticMemBase) - 1;
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
	public void writeByte(int byteAddr, int val)
	{
		checkWrite(byteAddr, val);
		mem.writeByte(byteAddr, val);
	}
	@Override
	public void writeWord(int byteAddr, int val)
	{
		writeByte(byteAddr, val >>> 8);
		writeByte(byteAddr + 1, val & 0xFF);
	}
	private void checkWrite(int byteAddr, int val)
	{
		if(!ignoreStaticMemWrite && byteAddr > lastDynamicMemByte)
			throw new MemoryException("Write to static / high memory");
		if(!ignoreNondynamicHeaderWrite && !headerParser.isAllowedAsDynamicWrite(byteAddr, val))
			throw new MemoryException("Write to non-dynamic header field");
	}
}