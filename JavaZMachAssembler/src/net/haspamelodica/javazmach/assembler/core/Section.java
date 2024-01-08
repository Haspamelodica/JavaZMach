package net.haspamelodica.javazmach.assembler.core;

public enum Section implements Location
{
	CODE,
	DICTIONARY,
	OBJ_TABLE,
	GLOBAL_VAR_TABLE,
	STATIC_MEM,
	ABBREV_TABLE,
	FILE_END,
}
