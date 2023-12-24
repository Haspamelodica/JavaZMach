package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.appendZString;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValue;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.throwShortOverrideButNotShort;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.varnumByteAndUpdateRoutine;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.versionRangeString;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.EXTENDED;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.LONG;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.SHORT;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.VARIABLE;
import static net.haspamelodica.javazmach.core.instructions.OpcodeKind.VAR;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.BranchInfo;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.Operand;
import net.haspamelodica.javazmach.assembler.model.SimpleBranchTarget;
import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.assembler.model.ZString;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.instructions.OpcodeForm;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledInstruction implements CodeLocation
{
	private final int version;

	private final Opcode					opcode;
	private final Optional<OpcodeForm>		formOverride;
	private final List<AssembledOperand>	operands;
	private final Optional<Variable>		storeTarget;
	private final Optional<BranchInfo>		branchInfo;
	private final Optional<ZString>			text;

	public AssembledInstruction(ZAssemblerInstruction instruction, int version, Map<String, Opcode> opcodesByNameLowercase)
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
				throw new IllegalArgumentException("Opcode " + instruction.opcode() + " doesn't exist in V" + version
						+ ", only " + existingVersionsThisName);
			// shouldn't really be possible - the grammar knows which opcodes there are.
			// Still, better safe than sorry - ZAssembler might theoretically be used without ZAssemblerParser,
			// and the way instructions are parsed also might change later.
			throw new IllegalArgumentException("Opcode " + instruction.opcode() + " unknown");
		}
		if(opcode.isStoreOpcode != instruction.storeTarget().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is store, but no store target was given: " + instruction);
		if(opcode.isBranchOpcode != instruction.branchInfo().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is branch, but no branch info was given: " + instruction);
		if(opcode.isTextOpcode != instruction.text().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is text, but no text was given: " + instruction);

		List<Operand> operandsUnassembled = instruction.operands();

		if(operandsUnassembled.size() < switch(opcode.range)
		{
			case OP0, OP2, VAR, EXT -> 0;
			case OP1 -> 1;
		})
			throw new IllegalArgumentException("Too few operands for " + opcode.range + " instruction; not encodeable: " + instruction);

		if(operandsUnassembled.size() > switch(opcode.range)
		{
			case OP0 -> 0;
			case OP1 -> 1;
			// yes, 4 / 8 even for OP2 - 4 / 8 operands are actually encodeable for OP2.
			// Case in point: je, which is OP2, takes up to 4 operands.
			case OP2, VAR, EXT -> opcode.hasTwoOperandTypeBytes ? 8 : 4;
		})
			throw new IllegalArgumentException("Too many operands for " + opcode.range + " instruction; not encodeable: " + instruction);

		if(operandsUnassembled.size() < opcode.minArgs || operandsUnassembled.size() > opcode.maxArgs)
			System.err.println("WARNING: Incorrect number of operands given for opcode " + opcode
					+ ": expected " + opcode.minArgs + (opcode.maxArgs != opcode.minArgs ? "-" + opcode.maxArgs : "")
					+ ", but was " + operandsUnassembled.size() + ": " + instruction);

		this.formOverride = instruction.form();
		this.operands = instruction.operands().stream().map(o -> switch(o)
		{
			case IntegralValue value -> new AssembledConstantOperand(value);
			case Variable variable -> new AssembledVariableOperand(variable);
		}).toList();
		this.storeTarget = instruction.storeTarget();
		this.branchInfo = instruction.branchInfo();
		this.text = instruction.text();
	}

	public int sizeEstimate()
	{
		//TODO
		return -1;
	}

	public void append(SequentialMemoryWriteAccess codeSeq, LabelResolver labelResolver)
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
					&& operands.get(0).typeEncodeableOneBit(labelResolver)
					&& operands.get(1).typeEncodeableOneBit(labelResolver)
							? LONG
							: VARIABLE;
			case VAR -> VARIABLE;
			case EXT -> EXTENDED;
		};

		if(formOverride.isPresent() && formOverride.get() != form)
			//TODO add case OP2 doesn't work if an operand is not a small constant to error message
			throw new IllegalArgumentException("Illegal form requested: kind " + opcode.range + " opcode with "
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
			case LONG -> codeSeq.writeNextByte(0
					// form LONG: bit 7 is 0.
					| (0 << 7)
					// kind: implicitly OP2.
					// operand type 1: bit 6
					| (operands.get(0).encodeTypeOneBitAssumePossible() << 6)
					// operand type 2: bit 5
					| (operands.get(1).encodeTypeOneBitAssumePossible() << 5)
					// opcode: bits 4-0.
					| (opcode.opcodeNumber << 0));
			case SHORT -> codeSeq.writeNextByte(0
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
				codeSeq.writeNextByte(0xbe);
				codeSeq.writeNextByte(opcode.opcodeNumber);
				appendEncodedOperandTypesVar(codeSeq);
			}
			case VARIABLE ->
			{
				codeSeq.writeNextByte(0
						// form VARIABLE: bits 7-6 are 0b11.
						| (0b11 << 6)
						// kind: bit 5; OP2 is 0, VAR is 1.
						| ((opcode.range == VAR ? 1 : 0) << 5)
						// opcode: bits 4-0.
						| (opcode.opcodeNumber << 0));
				appendEncodedOperandTypesVar(codeSeq);
			}
		}

		operands.forEach(operand -> operand.append(codeSeq, labelResolver));
		storeTarget.ifPresent(storeTarget -> codeSeq.writeNextByte(varnumByteAndUpdateRoutine(storeTarget)));
		branchInfo.ifPresent(branchInfo ->
		{
			switch(branchInfo.target())
			{
				case SimpleBranchTarget target ->
				{
					switch(target)
					{
						case rfalse -> appendEncodedBranchOffset(codeSeq, 0, branchInfo);
						case rtrue -> appendEncodedBranchOffset(codeSeq, 1, branchInfo);
					}
				}
				case IntegralValue target ->
				{
					BigInteger targetBigint = integralValue(target, labelResolver);
					BigInteger branchTargetEncoded = targetBigint.add(BigInteger.TWO);
					if(branchTargetEncoded.equals(BigInteger.ZERO) || branchTargetEncoded.equals(BigInteger.ONE))
						throw new IllegalArgumentException("A branch target of " + targetBigint
								+ " is not encodable as it would conflict with rtrue / rfalse");
					appendEncodedBranchOffset(codeSeq, bigintIntChecked(14, branchTargetEncoded,
							bte -> "Branch target out of range: " + targetBigint), branchInfo);
				}
				//TODO obsolete, but maybe something is worth savouring?
				/*
				case LabelReference target ->
				{
					// Here, we write a dummy value as the branch target, which will later be overwritten by the reference.
					// At first, optimistically assume the branch target can be assembled in short form, so choose 0 as the dummy value.
					// If it doesn't, the second byte will be inserted later when the reference is resolved.
					// Determining this beforehand would be hard because whether we can assemble this in the short form
					// depends on where the label refers to, and for the labels where it matters this even
					// will only become known in the future.
					CodeLocation codeLocationBeforeBranchOffset = codeLocationHere();
					appendEncodedBranchOffset(0, branchInfo);
					CodeLocation codeLocationAfterBranchOffset = codeLocationHere();
				
					references.add(new Reference(new BranchTarget(codeLocationBeforeBranchOffset, branchInfo.branchLengthOverride()),
							new CodeLabelRelativeReference(target.name(), codeLocationAfterBranchOffset)));
				}
				*/
			};
		});

		text.ifPresent(text -> appendZString(codeSeq, text, version));
	}

	private void checkOpcodeNumberMask(Opcode opcode, int mask, OpcodeForm form)
	{
		if((opcode.opcodeNumber & mask) != opcode.opcodeNumber)
			throw new IllegalArgumentException("Opcode " + opcode
					+ " should be assembled as " + form + ", but has an opcode number greater than 0x"
					+ Integer.toHexString(mask) + ": " + opcode.opcodeNumber);
	}

	private void appendEncodedOperandTypesVar(SequentialMemoryWriteAccess codeSeq)
	{
		int operandTypesEncoded = 0;
		int i;
		for(i = 0; i < operands.size(); i ++)
			operandTypesEncoded = (operandTypesEncoded << 2) | operands.get(i).encodeTypeTwoBits();
		// the rest is omitted, which is encoded as 0b11
		for(; i < (opcode.hasTwoOperandTypeBytes ? 8 : 4); i ++)
			operandTypesEncoded = (operandTypesEncoded << 2) | 0b11;

		if(opcode.hasTwoOperandTypeBytes)
			codeSeq.writeNextWord(operandTypesEncoded);
		else
			codeSeq.writeNextByte(operandTypesEncoded);
	}

	private void appendEncodedBranchOffset(SequentialMemoryWriteAccess codeSeq, int branchOffsetEncodedOrZero, BranchInfo info)
	{
		boolean isValueShort = isBranchOffsetShort(branchOffsetEncodedOrZero);
		boolean isShort;
		if(info.branchLengthOverride().isEmpty())
			isShort = isValueShort;
		else
			isShort = switch(info.branchLengthOverride().get())
			{
				case LONGBRANCH -> false;
				case SHORTBRANCH ->
				{
					if(!isValueShort)
						yield throwShortOverrideButNotShort(branchOffsetEncodedOrZero);
					yield true;
				}
			};

		codeSeq.writeNextByte(0
				// branch-on-condition-false: bit 7; on false is 0, on true is 1.
				| ((info.branchOnConditionFalse() ? 0 : 1) << 7)
				// branch offset encoding: bit 6; long is 0, short is 1.
				| ((isShort ? 1 : 0) << 6)
				| (branchOffsetEncodedOrZero >> (isShort ? 0 : 8)));
		if(!isShort)
			codeSeq.writeNextByte(branchOffsetEncodedOrZero & 0xff);
	}

	private static boolean isBranchOffsetShort(int value)
	{
		return value == (value & ((1 << 6) - 1));
	}
}
