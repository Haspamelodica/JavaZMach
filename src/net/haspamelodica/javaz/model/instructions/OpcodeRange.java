package net.haspamelodica.javaz.model.instructions;

public enum OpcodeRange
{
	OP0(OperandCount.OP0),
	OP1(OperandCount.OP1),
	OP2(OperandCount.OP2),
	VAR(OperandCount.VAR),
	EXT(null);

	public final OperandCount asCount;

	private OpcodeRange(OperandCount asCount)
	{
		this.asCount = asCount;
	}
}