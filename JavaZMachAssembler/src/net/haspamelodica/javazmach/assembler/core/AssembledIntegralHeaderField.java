package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.memory.WritableMemory;

public interface AssembledIntegralHeaderField
{
	public void assemble(WritableMemory header, LocationAndLabelResolver locationResolver);
	public HeaderField getField();
}
