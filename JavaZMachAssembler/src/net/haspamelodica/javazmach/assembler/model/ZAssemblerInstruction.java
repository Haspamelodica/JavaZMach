package net.haspamelodica.javazmach.assembler.model;

import java.util.List;
import java.util.Optional;

import net.haspamelodica.javazmach.core.instructions.OpcodeForm;

public record ZAssemblerInstruction(String opcode, Optional<OpcodeForm> form, List<Operand> operands, Optional<Variable> storeTarget,
		Optional<BranchInfo> branchInfo, Optional<ZString> text) implements MacroOrFileEntry
{
	public ZAssemblerInstruction(String opcode, Optional<OpcodeForm> form, List<Operand> operands, Optional<Variable> storeTarget,
			Optional<BranchInfo> branchInfo, Optional<ZString> text)
	{
		this.opcode = opcode;
		this.form = form;
		this.operands = List.copyOf(operands);
		this.storeTarget = storeTarget;
		this.branchInfo = branchInfo;
		this.text = text;
	}
}
