package net.haspamelodica.javaz.model.instructions;

public enum OpcodeForm
{
	LONG,
	SHORT,
	EXTENDED,
	VARIABLE;

	public static OpcodeForm decode(int opcodeByte)
	{
		return opcodeByte == 0xBE ? EXTENDED : (opcodeByte & 0x80) == 0 ? //bit 7
				LONG : (opcodeByte & 0x40) == 0 ? SHORT : VARIABLE;//bit 6
	}
}