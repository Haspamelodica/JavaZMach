package net.haspamelodica.javazmach.assembler.core;

public class CodeLocation implements Comparable<CodeLocation>
{
	private int relAddr;

	public CodeLocation(int relAddr)
	{
		this.relAddr = relAddr;
	}

	public int relAddr()
	{
		return relAddr;
	}

	public void bytesInserted(int insertedByteRelAddr, int insertedByteCount)
	{
		if(relAddr >= insertedByteRelAddr)
			relAddr += insertedByteCount;
	}

	@Override
	public int compareTo(CodeLocation o)
	{
		return Integer.compare(this.relAddr, o.relAddr);
	}
}
