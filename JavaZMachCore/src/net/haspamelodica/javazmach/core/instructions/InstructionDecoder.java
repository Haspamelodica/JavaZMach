package net.haspamelodica.javazmach.core.instructions;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryAccess;

public class InstructionDecoder
{
	private final int version;

	private final boolean	ignoreUnknownInstructions;
	private final boolean	ignoreIncorrectVersion;
	private final boolean	ignoreVarOperandsGaps;
	private final boolean	ignoreIncorrectOperandsCount;

	private final SequentialMemoryAccess mem;


	public InstructionDecoder(GlobalConfig config, int version, SequentialMemoryAccess mem)
	{
		this.version = version;

		this.ignoreUnknownInstructions = config.getBool("instructions.decoding.ignore_unknown_instructions");
		this.ignoreIncorrectVersion = config.getBool("instructions.decoding.ignore_incorrect_version");
		this.ignoreVarOperandsGaps = config.getBool("instructions.decoding.operands.ignore_var_operands_gaps");
		this.ignoreIncorrectOperandsCount = config.getBool("instructions.decoding.operands.ignore_incorrect_operand_count");

		this.mem = mem;
	}

	/**
	 * Text following the instruction is not read!
	 */
	public void decode(DecodedInstruction target)
	{
		int opcodeByte = mem.readNextByte();

		OpcodeForm form = OpcodeForm.decode(opcodeByte, version);
		target.form = form;

		OpcodeKind kind = OpcodeKind.decode(opcodeByte, form);

		Opcode opcode;
		if(form == OpcodeForm.EXTENDED)
			opcode = Opcode.decodeExtended(mem.readNextByte(), version);
		else
			opcode = Opcode.decode(opcodeByte, form, kind, version);
		target.opcode = opcode;
		if(opcode == Opcode._unknown_instr && !ignoreUnknownInstructions)
			throw new InstructionFormatException("Unknown instruction: " + opcodeByte);
		if((opcode.minVersion > version || (opcode.maxVersion > 0 && opcode.maxVersion < version)) && !ignoreIncorrectVersion)
			throw new InstructionFormatException("Instruction not valid for version " + version +
					" (V" + opcode.minVersion + (opcode.maxVersion > 0 ? "-" + opcode.maxVersion : "+") + "): " + opcode);
		switch(kind)
		{
			case OP0:
				target.operandCount = 0;
				break;
			case OP1:
				target.operandCount = 1;
				OperandType operandType = OperandType.decodeTwoBits((opcodeByte & 0x30) >>> 4);//bits 5-4
				target.operandTypes[0] = operandType;
				readOperand(operandType, 0, target.operandValues);
				break;
			case OP2:
				switch(form)
				{
					case LONG:
						target.operandCount = 2;
						OperandType operandType1 = OperandType.decodeOneBit((opcodeByte & 0x40) >>> 6);//bit 6
						OperandType operandType2 = OperandType.decodeOneBit((opcodeByte & 0x20) >>> 5);//bit 5
						target.operandTypes[0] = operandType1;
						target.operandTypes[1] = operandType2;
						readOperand(operandType1, 0, target.operandValues);
						readOperand(operandType2, 1, target.operandValues);
						break;
					case VARIABLE:
						decodeVarParams(false, target);
						break;
					case SHORT:
					case EXTENDED:
						throw new IllegalStateException("Impossible combination of OperandCount and OpcodeForm: " + kind + " - " + form);
					default:
						throw new IllegalArgumentException("Unknown enum type: " + form);
				}
				break;
			case VAR:
			case EXT:
				decodeVarParams(opcode.hasTwoOperandTypeBytes, target);
				break;
			default:
				throw new IllegalArgumentException("Unknown enum type: " + kind);
		}

		if(!ignoreIncorrectOperandsCount && opcode != Opcode._unknown_instr
				&& (target.operandCount < opcode.minArgs || target.operandCount > opcode.maxArgs))
			throw new InstructionFormatException("Too many / few operands (" + target.operandCount + ") for " + opcode);

		if(opcode.isStoreOpcode)
			target.storeTarget = mem.readNextByte();
		if(opcode.isBranchOpcode)
		{
			int branchOffsetByte = mem.readNextByte();
			target.branchOnConditionFalse = (branchOffsetByte & 0x80) == 0;//bit 7
			target.branchConditionShort = (branchOffsetByte & 0x40) != 0;//bit 6
			if(target.branchConditionShort)
				target.branchOffset = branchOffsetByte & 0x3F;//bits 5-0
			else
				// The shifts by 18 effectively sign-extend 14 to 32 bit.
				target.branchOffset = ((mem.readNextByte() | ((branchOffsetByte & 0x3F) << 8)) << 18) >> 18;//bits 5-0
		}
	}

	private void decodeVarParams(boolean hasTwoOperandTypeBytes, DecodedInstruction target)
	{
		int operandTypes;
		int bitLocation;
		if(hasTwoOperandTypeBytes)
		{
			operandTypes = mem.readNextWord();
			bitLocation = 14;
		} else
		{
			operandTypes = mem.readNextByte();
			bitLocation = 6;
		}
		boolean followingOperandsMustBeOmitted = false;
		int operandI;
		for(operandI = 0; bitLocation >= 0; bitLocation -= 2)
		{
			OperandType type = OperandType.decodeTwoBits((operandTypes >>> bitLocation) & 0x03);
			if(type != null)
				if(followingOperandsMustBeOmitted)
					throw new InstructionFormatException("Operands after the first omitted operand found");
				else
				{
					target.operandTypes[operandI] = type;
					readOperand(type, operandI ++, target.operandValues);
				}
			else
				followingOperandsMustBeOmitted = !ignoreVarOperandsGaps;
		}
		target.operandCount = operandI;
	}
	private void readOperand(OperandType operandType, int valIndex, int[] valsArray)
	{
		switch(operandType)
		{
			case LARGE_CONST:
				valsArray[valIndex] = mem.readNextWord();
				break;
			case SMALL_CONST:
			case VARIABLE:
				valsArray[valIndex] = mem.readNextByte();
				break;
			default:
				throw new IllegalArgumentException("Unknown enum type: " + operandType);
		}
	}
}
