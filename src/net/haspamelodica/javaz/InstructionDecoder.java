package net.haspamelodica.javaz;

public class InstructionDecoder
{
	private final GlobalConfig config;

	public InstructionDecoder(GlobalConfig config)
	{
		this.config = config;
	}

	/**
	 * Decodes a Z-Instruction in <code>mem</code> starting from <code>addr</code> and stores the result
	 * to <code>target</code> and returns the next address after this instruction.
	 */
	public int decode(Memory mem, int addr, int version, DecodedInstruction target)
	{
		int opcodeByte = mem.readByte(addr ++);

		OpcodeForm form = OpcodeForm.decode(opcodeByte);
		target.form = form;

		OperandCount count = OperandCount.decode(opcodeByte, form);

		Opcode opcode;
		if(form == OpcodeForm.EXTENDED)
			opcode = Opcode.decodeExtended(mem.readByte(addr ++), version);
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
				addr = readOperand(mem, addr, operandType, 0, target.operandValues);
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
						addr = readOperand(mem, addr, operandType1, 0, target.operandValues);
						addr = readOperand(mem, addr, operandType2, 1, target.operandValues);
						break;
					case VARIABLE:
						addr = decodeVarParams(mem, addr, false, target);
						//TODO
						break;
					case SHORT:
					case EXTENDED:
						throw new IllegalStateException("Impossible combination of OperandCount and OpcodeForm: " + count + " - " + form);
					default:
						throw new IllegalArgumentException("Unknown enum type: " + form);
				}
				break;
			case VAR:
				addr = decodeVarParams(mem, addr, opcode.hasTwoOperandTypeBytes, target);
			default:
				throw new IllegalArgumentException("Unknown enum type: " + count);
		}

		if(opcode.isStoreOpcode)
			target.storeTarget = mem.readByte(addr ++);
		if(opcode.isBranchOpcode)
		{
			int branchOffsetByte = mem.readByte(addr ++);
			target.branchOnConditionTrue = (branchOffsetByte & 0x80) != 0;//bit 7
			if((branchOffsetByte & 0x40) == 0)
				target.branchOffset = mem.readByte(addr ++) | ((branchOffsetByte & 0x3) << 8);//bits 5-0
			else
				target.branchOffset = branchOffsetByte & 0x3F;//bits 5-0
		}
		//TODO text

		return addr;
	}

	private int decodeVarParams(Memory mem, int addr, boolean hasTwoOperandTypeBytes, DecodedInstruction target)
	{
		int operandTypes;
		int firstOperandBitLocation;
		if(hasTwoOperandTypeBytes)
		{
			operandTypes = mem.readWord(addr);
			addr += 2;
			firstOperandBitLocation = 14;
		} else
		{
			operandTypes = mem.readByte(addr ++);
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
					addr = readOperand(mem, addr, type, operandI ++, target.operandValues);
				}
			else
				followingOperandsMustBeOmitted = checkOperandsAreInBlock;
		}
		return addr;
	}
	private static int readOperand(Memory mem, int addr, OperandType operandType, int valIndex, int[] valsArray)
	{
		switch(operandType)
		{
			case LARGE_CONST:
				valsArray[valIndex] = mem.readWord(addr);
				return addr + 2;
			case SMALL_CONST:
			case VARIABLE:
				valsArray[valIndex] = mem.readByte(addr);
				return addr + 1;
			default:
				throw new IllegalArgumentException("Unknown enum type: " + operandType);
		}
	}
}