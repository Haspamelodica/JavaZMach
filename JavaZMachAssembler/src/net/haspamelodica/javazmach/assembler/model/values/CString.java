package net.haspamelodica.javazmach.assembler.model.values;

public record CString(String value) implements ByteSequenceElement
{
	public CString append(String b)
	{
		return new CString(value + b);
	}
}
