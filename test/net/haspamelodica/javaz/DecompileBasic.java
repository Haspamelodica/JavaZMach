package net.haspamelodica.javaz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.instructions.DecodedInstruction;
import net.haspamelodica.javaz.core.instructions.InstructionDecoder;
import net.haspamelodica.javaz.core.instructions.Opcode;
import net.haspamelodica.javaz.core.instructions.OperandType;
import net.haspamelodica.javaz.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javaz.core.memory.WritableFixedSizeMemory;
import net.haspamelodica.javaz.core.memory.WritableMemory;
import net.haspamelodica.javaz.core.text.ZCharsAlphabet;
import net.haspamelodica.javaz.core.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javaz.core.text.ZCharsToZSCIIConverter;

public class DecompileBasic
{
	public static void main(String[] args) throws IOException
	{
		int version = 3;
		GlobalConfig config = new GlobalConfig();
		WritableMemory mem = new WritableFixedSizeMemory(Files.readAllBytes(Paths.get("ZORK1.z3")));
		HeaderParser header = new HeaderParser(mem);
		SequentialMemoryAccess memSeq = new SequentialMemoryAccess(mem);
		InstructionDecoder decoder = new InstructionDecoder(config, version, memSeq);
		ZCharsToZSCIIConverter textConverter = new ZCharsToZSCIIConverter(config, version, header, mem, new ZCharsAlphabet(config, version, header, mem), new ZCharsSeqMemUnpacker(memSeq));
		textConverter.reset();
		memSeq.setAddress(header.getField(HeaderParser.InitialPCLoc));
		memSeq.setAddress(0x535E);//address of Overview example
		DecodedInstruction instr = new DecodedInstruction();
		do
		{
			System.out.print("pc ");
			System.out.printf("%06x-", memSeq.getAddress());
			decoder.decode(instr);
			System.out.printf("%06x", memSeq.getAddress() - 1);
			System.out.print(": ");
			System.out.print(instr);
			if(instr.opcode.isBranchOpcode && instr.branchOffset != 0 && instr.branchOffset != 1)
				System.out.printf(" (branch to 0x%04x)", memSeq.getAddress() + (((instr.branchOffset - 2) << 16) >> 16));
			if(instr.opcode == Opcode.jump && instr.operandTypes[0] != OperandType.VARIABLE && instr.operandValues[0] != 0 && instr.operandValues[0] != 1)
				System.out.printf(" (jump to 0x%04x)", memSeq.getAddress() + (((instr.operandValues[0] - 2) << 16) >> 16));
			if(instr.opcode.isTextOpcode)
			{
				System.out.print(" text: \"");
				textConverter.decode(zsciiChar -> System.out.print((char) zsciiChar));
				System.out.print('"');
			}
			System.out.println();
		} while(instr.opcode != Opcode._unknown_instr);
	}
}