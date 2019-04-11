package net.haspamelodica.javaz;

import static net.haspamelodica.javaz.core.HeaderParser.VersionLoc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.instructions.DecodedInstruction;
import net.haspamelodica.javaz.core.instructions.InstructionDecoder;
import net.haspamelodica.javaz.core.instructions.Opcode;
import net.haspamelodica.javaz.core.instructions.OperandType;
import net.haspamelodica.javaz.core.memory.CopyOnWriteMemory;
import net.haspamelodica.javaz.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javaz.core.memory.StaticArrayBackedMemory;
import net.haspamelodica.javaz.core.text.ZCharsAlphabet;
import net.haspamelodica.javaz.core.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javaz.core.text.ZCharsToZSCIIConverter;

public class DecompileBasic
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = new GlobalConfig();
		CopyOnWriteMemory mem = new CopyOnWriteMemory(new StaticArrayBackedMemory(Files.readAllBytes(Paths.get("storyfiles/zork1.z3"))));
		HeaderParser header = new HeaderParser(mem);
		SequentialMemoryAccess memSeq = new SequentialMemoryAccess(mem);
		int version = header.getField(VersionLoc);
		InstructionDecoder decoder = new InstructionDecoder(config, version, memSeq);
		ZCharsAlphabet alphabet = new ZCharsAlphabet(config, version, header, mem);
		ZCharsToZSCIIConverter textConverter = new ZCharsToZSCIIConverter(config, version, header, mem, alphabet, new ZCharsSeqMemUnpacker(memSeq));
		alphabet.reset();
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