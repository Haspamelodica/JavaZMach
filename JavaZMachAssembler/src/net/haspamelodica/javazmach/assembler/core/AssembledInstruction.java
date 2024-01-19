package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.appendZString;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.varnumByteAndUpdateRoutine;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.versionRangeString;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.EXTENDED;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.LONG;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.SHORT;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.VARIABLE;
import static net.haspamelodica.javazmach.core.instructions.OpcodeKind.VAR;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.Operand;
import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.assembler.model.ZString;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.instructions.OpcodeForm;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledInstruction implements AssembledEntry
{
	private final int version;

	private final Opcode						opcode;
	private final Optional<OpcodeForm>			formOverride;
	private final List<AssembledOperand>		operands;
	private final Optional<Variable>			storeTarget;
	private final Optional<AssembledBranchInfo>	branchInfo;
	private final Optional<ZString>				text;

	public AssembledInstruction(MacroContext macroContext, ZAssemblerInstruction instruction, int version, Map<String, Opcode> opcodesByNameLowercase)
	{
		this.version = version;

		this.opcode = opcodesByNameLowercase.get(instruction.opcode().toLowerCase());
		if(opcode == null)
		{
			String existingVersionsThisName = Arrays.stream(Opcode.values())
					.filter(opcode2 -> opcode2.name.toLowerCase().equals(instruction.opcode().toLowerCase()))
					.map(opcode2 -> versionRangeString(opcode2.minVersion, opcode2.maxVersion))
					.collect(Collectors.joining(", "));
			if(!existingVersionsThisName.isEmpty())
				defaultError("Opcode " + instruction.opcode() + " doesn't exist in V" + version
						+ ", only " + existingVersionsThisName);
			// shouldn't really be possible - the grammar knows which opcodes there are.
			// Still, better safe than sorry - ZAssembler might theoretically be used without ZAssemblerParser,
			// and the way instructions are parsed also might change later.
			defaultError("Opcode " + instruction.opcode() + " unknown");
		}
		if(opcode.isStoreOpcode != instruction.storeTarget().isPresent())
			defaultError("Opcode " + opcode + " is store, but no store target was given: " + instruction);
		if(opcode.isBranchOpcode != instruction.branchInfo().isPresent())
			defaultError("Opcode " + opcode + " is branch, but no branch info was given: " + instruction);
		if(opcode.isTextOpcode != instruction.text().isPresent())
			defaultError("Opcode " + opcode + " is text, but no text was given: " + instruction);

		List<Operand> operandsUnassembled = instruction.operands();

		if(operandsUnassembled.size() < switch(opcode.range)
		{
			case OP0, OP2, VAR, EXT -> 0;
			case OP1 -> 1;
		})
			defaultError("Too few operands for " + opcode.range + " instruction; not encodeable: " + instruction);

		if(operandsUnassembled.size() > switch(opcode.range)
		{
			case OP0 -> 0;
			case OP1 -> 1;
			// yes, 4 / 8 even for OP2 - 4 / 8 operands are actually encodeable for OP2.
			// Case in point: je, which is OP2, takes up to 4 operands.
			case OP2, VAR, EXT -> opcode.hasTwoOperandTypeBytes ? 8 : 4;
		})
			defaultError("Too many operands for " + opcode.range + " instruction; not encodeable: " + instruction);

		if(operandsUnassembled.size() < opcode.minArgs || operandsUnassembled.size() > opcode.maxArgs)
			// no need to go through custom DiagnosticHandler: won't change in later iterations
			defaultWarning("Incorrect number of operands given for opcode " + opcode
					+ ": expected " + opcode.minArgs + (opcode.maxArgs != opcode.minArgs ? "-" + opcode.maxArgs : "")
					+ ", but was " + operandsUnassembled.size() + ": " + instruction);

		this.formOverride = instruction.form();
		boolean formOverriddenToLONG = formOverride.isPresent() && formOverride.get() == LONG;
		this.operands = instruction.operands().stream().map(o -> switch(macroContext.resolve(o))
		{
			case IntegralValue value -> new AssembledImmediateOperand(macroContext, value, formOverriddenToLONG);
			case Variable variable -> new AssembledVariableOperand(variable);
		}).toList();
		this.storeTarget = instruction.storeTarget();
		this.branchInfo = instruction.branchInfo().map(branchInfo -> new AssembledBranchInfo(macroContext, branchInfo, new BranchOriginLocation(this)));
		this.text = instruction.text();
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		operands.forEach(operand -> operand.updateResolvedValue(valueReferenceResolver));
		branchInfo.ifPresent(branchInfo -> branchInfo.updateResolvedTarget(valueReferenceResolver));
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		OpcodeForm form = switch(opcode.range)
		{
			case OP0 -> SHORT;
			case OP1 -> SHORT;
			case OP2 -> true
					&& formOverride.orElse(LONG) == LONG
				// yes, we need to check operand count even though we know the form is OP2:
				// for example, je is OP2, but can take any number between 1 and 4 of operands.
					&& operands.size() == 2
					&& operands.get(0).typeEncodeableOneBit()
					&& operands.get(1).typeEncodeableOneBit()
							? LONG
							: VARIABLE;
			case VAR -> VARIABLE;
			case EXT -> EXTENDED;
		};

		if(formOverride.isPresent() && formOverride.get() != form)
			defaultError("Illegal form requested: kind " + opcode.range + " opcode with "
					+ operands.size() + " operands, but requested was form " + formOverride.get());

		// There are no opcodes which would trigger this, but let's be paranoid.
		checkOpcodeNumberMask(opcode, switch(form)
		{
			case LONG, VARIABLE -> 0x1f;
			case SHORT -> 0x0f;
			case EXTENDED -> 0xff;
		}, form);

		switch(form)
		{
			case LONG -> memSeq.writeNextByte(0
					// form LONG: bit 7 is 0.
					| (0 << 7)
					// kind: implicitly OP2.
					// operand type 1: bit 6
					| (operands.get(0).encodeTypeOneBitAssumePossible() << 6)
					// operand type 2: bit 5
					| (operands.get(1).encodeTypeOneBitAssumePossible() << 5)
					// opcode: bits 4-0.
					| (opcode.opcodeNumber << 0));
			case SHORT -> memSeq.writeNextByte(0
					// form SHORT: bits 7-6 are 0b10.
					| (0b10 << 6)
					// kind: implicitly OP0 / OP1, depending on operand type: omitted means OP0.
					// No need to check this here; operand count is already checked above.
					// operand type (if present): bits 5-4
					| ((operands.size() == 0 ? 0b11 : operands.get(0).encodeTypeTwoBits()) << 4)
					// opcode: bits 3-0.
					| (opcode.opcodeNumber << 0));
			case EXTENDED ->
			{
				// EXTENDED form only exists in V5+, but let's rely on the Opcode enum being sane and declaring all EXT opcodes as V5+.
				memSeq.writeNextByte(0xbe);
				memSeq.writeNextByte(opcode.opcodeNumber);
				appendEncodedOperandTypesVar(memSeq);
			}
			case VARIABLE ->
			{
				memSeq.writeNextByte(0
						// form VARIABLE: bits 7-6 are 0b11.
						| (0b11 << 6)
						// kind: bit 5; OP2 is 0, VAR is 1.
						| ((opcode.range == VAR ? 1 : 0) << 5)
						// opcode: bits 4-0.
						| (opcode.opcodeNumber << 0));
				appendEncodedOperandTypesVar(memSeq);
			}
		}

		operands.forEach(operand -> operand.append(memSeq, diagnosticHandler));
		storeTarget.ifPresent(storeTarget -> memSeq.writeNextByte(varnumByteAndUpdateRoutine(storeTarget)));
		branchInfo.ifPresent(branchInfo -> branchInfo.appendChecked(memSeq, diagnosticHandler));
		// Branches are relative to after the branch data.
		locationEmitter.emitLocationHere(new BranchOriginLocation(this));
		text.ifPresent(text -> appendZString(memSeq, text, version));
	}

	private void checkOpcodeNumberMask(Opcode opcode, int mask, OpcodeForm form)
	{
		if((opcode.opcodeNumber & mask) != opcode.opcodeNumber)
			defaultError("Opcode " + opcode
					+ " should be assembled as " + form + ", but has an opcode number greater than 0x"
					+ Integer.toHexString(mask) + ": " + opcode.opcodeNumber);
	}

	private void appendEncodedOperandTypesVar(SequentialMemoryWriteAccess memSeq)
	{
		int operandTypesEncoded = 0;
		int i;
		for(i = 0; i < operands.size(); i ++)
			operandTypesEncoded = (operandTypesEncoded << 2) | operands.get(i).encodeTypeTwoBits();
		// the rest is omitted, which is encoded as 0b11
		for(; i < (opcode.hasTwoOperandTypeBytes ? 8 : 4); i ++)
			operandTypesEncoded = (operandTypesEncoded << 2) | 0b11;

		if(opcode.hasTwoOperandTypeBytes)
			memSeq.writeNextWord(operandTypesEncoded);
		else
			memSeq.writeNextByte(operandTypesEncoded);
	}
}
