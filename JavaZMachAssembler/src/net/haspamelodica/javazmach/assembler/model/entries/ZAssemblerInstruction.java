package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.List;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.entries.instruction.BranchInfo;
import net.haspamelodica.javazmach.assembler.model.values.Operand;
import net.haspamelodica.javazmach.assembler.model.values.StoreTarget;
import net.haspamelodica.javazmach.assembler.model.values.ZString;
import net.haspamelodica.javazmach.core.instructions.OpcodeForm;

public record ZAssemblerInstruction(String opcode, Optional<OpcodeForm> form, List<Operand> operands, Optional<StoreTarget> storeTarget,
		Optional<BranchInfo> branchInfo, Optional<ZString> text) implements MacroOrFileEntry
{
	public ZAssemblerInstruction(String opcode, Optional<OpcodeForm> form, List<Operand> operands, Optional<StoreTarget> storeTarget,
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
