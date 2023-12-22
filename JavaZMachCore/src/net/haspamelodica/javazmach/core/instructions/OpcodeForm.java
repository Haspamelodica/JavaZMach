package net.haspamelodica.javazmach.core.instructions;

public enum OpcodeForm
{
	LONG("lng"),
	SHORT("srt"),
	EXTENDED("ext"),
	VARIABLE("var");

	public final String shortName;

	private OpcodeForm(String shortName)
	{
		this.shortName = shortName;
	}

	public static OpcodeForm decode(int opcodeByte, int version)
	{
		return opcodeByte == 0xBE && version >= 5 ? EXTENDED : (opcodeByte & 0x80) == 0 ? //bit 7
				LONG : (opcodeByte & 0x40) == 0 ? SHORT : VARIABLE;//bit 6
	}
}
