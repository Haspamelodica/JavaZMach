package net.haspamelodica.javazmach.assembler.core;

import java.util.List;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.BranchInfo;
import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.assembler.model.ZString;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.instructions.OpcodeForm;

public class AssembledInstruction
{
	private final Opcode					opcode;
	private final Optional<OpcodeForm>		formOverride;
	private final List<AssembledOperand>	operands;
	private final Optional<Variable>		storeTarget;
	private final Optional<BranchInfo>		branchInfo;
	private final Optional<ZString>			text;

	public AssembledInstruction(ZAssemblerInstruction instruction, ValueAssembler valueAssembler)
	{
		//TODO
		operands = null;
	}

	public int sizeEstimate()
	{
		//TODO
		return -1;
	}
}
