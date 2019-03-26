package net.haspamelodica.javaz.model.instructions;

public enum OperandCount
{
	OP0,
	OP1,
	OP2,
	VAR;

	public static OperandCount decode(int opcodeByte, OpcodeForm form)
	{
		switch(form)
		{
			case LONG:
				return OP2;
			case SHORT:
				return (opcodeByte & 0x30) == 0x30 ? OP0 : OP1;//bits 5-4
			case EXTENDED:
				return VAR;
			case VARIABLE:
				return (opcodeByte & 0x20) == 0 ? OP2 : VAR;//bit 5
			default:
				throw new IllegalArgumentException("Unknown enum type: " + form);
		}
	}
}