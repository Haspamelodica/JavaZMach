package net.haspamelodica.javaz;

public class InstructionDecoder
{
	private final GlobalConfig				config;
	private final int						version;
	private final SequentialMemoryAccess	mem;

	private final ZCharsUnpacker textUnpacker;

	public InstructionDecoder(GlobalConfig config, int version, SequentialMemoryAccess mem)
	{
		this.config = config;
		this.version = version;

		this.mem = mem;
		this.textUnpacker = new ZCharsUnpacker(mem);
	}

	public void decode(DecodedInstruction target)
	{
		int opcodeByte = mem.readNextByte();

		OpcodeForm form = OpcodeForm.decode(opcodeByte);
		target.form = form;

		OperandCount count = OperandCount.decode(opcodeByte, form);

		Opcode opcode;
		if(form == OpcodeForm.EXTENDED)
			opcode = Opcode.decodeExtended(mem.readNextByte(), version);
		else
			opcode = Opcode.decode(opcodeByte, form, count, version);
		target.opcode = opcode;
		if(opcode == Opcode._unknown_instr && config.getBool("instructions.decoding.dont_ignore_unknown_instructions"))
			throw new InstructionFormatException("Unknown instruction: " + opcodeByte);
		if((opcode.minVersion > version || (opcode.maxVersion > 0 && opcode.maxVersion < version)) && config.getBool("instructions.decoding.check_instruction_version"))
			throw new InstructionFormatException("Instruction not valid for version " + version +
					" (V" + opcode.minVersion + (opcode.maxVersion > 0 ? "-" + opcode.maxVersion : "+") + "): " + opcode);
		switch(count)
		{
			case OP0:
				target.operandCount = 0;
				break;
			case OP1:
				target.operandCount = 1;
				OperandType operandType = OperandType.decodeTwoBits((opcodeByte & 0x30) >> 4);//bits 5-4
				target.operandTypes[0] = operandType;
				readOperand(operandType, 0, target.operandValues);
				break;
			case OP2:
				target.operandCount = 2;
				switch(form)
				{
					case LONG:
						OperandType operandType1 = OperandType.decodeOneBit((opcodeByte & 0x40) >> 6);//bit 6
						OperandType operandType2 = OperandType.decodeOneBit((opcodeByte & 0x20) >> 5);//bit 5
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
						throw new IllegalStateException("Impossible combination of OperandCount and OpcodeForm: " + count + " - " + form);
					default:
						throw new IllegalArgumentException("Unknown enum type: " + form);
				}
				break;
			case VAR:
				decodeVarParams(opcode.hasTwoOperandTypeBytes, target);
			default:
				throw new IllegalArgumentException("Unknown enum type: " + count);
		}

		if(opcode.isStoreOpcode)
			target.storeTarget = mem.readNextByte();
		if(opcode.isBranchOpcode)
		{
			int branchOffsetByte = mem.readNextByte();
			target.branchOnConditionTrue = (branchOffsetByte & 0x80) != 0;//bit 7
			if((branchOffsetByte & 0x40) == 0)//bit 6
				target.branchOffset = mem.readNextByte() | ((branchOffsetByte & 0x3) << 8);//bits 5-0
			else
				target.branchOffset = branchOffsetByte & 0x3F;//bits 5-0
		}
		if(opcode.isTextOpcode)
			textUnpacker.unpack(target.text);
	}

	private void decodeVarParams(boolean hasTwoOperandTypeBytes, DecodedInstruction target)
	{
		int operandTypes;
		int firstOperandBitLocation;
		if(hasTwoOperandTypeBytes)
		{
			operandTypes = mem.readNextWord();
			firstOperandBitLocation = 14;
		} else
		{
			operandTypes = mem.readNextByte();
			firstOperandBitLocation = 6;
		}
		boolean checkOperandsAreInBlock = config.getBool("instructions.decoding.check_var_operands_block");
		boolean followingOperandsMustBeOmitted = false;
		for(int operandI = 0, bitLocation = firstOperandBitLocation; bitLocation >= 0; bitLocation -= 2)
		{
			OperandType type = OperandType.decodeTwoBits((operandTypes >> bitLocation) & 0x03);
			if(type != null)
				if(followingOperandsMustBeOmitted)
					throw new InstructionFormatException("Operands after the first omitted operand found");
				else
				{
					target.operandTypes[operandI] = type;
					readOperand(type, operandI ++, target.operandValues);
				}
			else
				followingOperandsMustBeOmitted = checkOperandsAreInBlock;
		}
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