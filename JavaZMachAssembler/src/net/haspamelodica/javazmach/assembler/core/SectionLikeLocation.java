package net.haspamelodica.javazmach.assembler.core;

public enum SectionLikeLocation implements ValueReference
{
	STATIC_MEM_BASE,
	HIGH_MEM_BASE,
	FILE_END,
	FILE_CHECKSUM;
}
