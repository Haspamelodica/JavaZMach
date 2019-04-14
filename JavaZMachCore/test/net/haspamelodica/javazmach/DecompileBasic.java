package net.haspamelodica.javazmach;

import static net.haspamelodica.javazmach.core.header.HeaderField.InitialPC15;
import static net.haspamelodica.javazmach.core.header.HeaderField.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.instructions.DecodedInstruction;
import net.haspamelodica.javazmach.core.instructions.InstructionDecoder;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.instructions.OperandType;
import net.haspamelodica.javazmach.core.memory.CopyOnWriteMemory;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javazmach.core.memory.StaticArrayBackedMemory;
import net.haspamelodica.javazmach.core.text.ZCharStream;
import net.haspamelodica.javazmach.core.text.ZCharsAlphabet;
import net.haspamelodica.javazmach.core.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javazmach.core.text.ZCharsToZSCIIConverterStream;

public class DecompileBasic
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = new GlobalConfig();
		CopyOnWriteMemory mem = new CopyOnWriteMemory(new StaticArrayBackedMemory(Files.readAllBytes(Paths.get("../storyfiles/zork1.z3"))));
		HeaderParser header = new HeaderParser(config, HeaderParser.getFieldUnchecked(mem, Version), mem);
		SequentialMemoryAccess memSeq = new SequentialMemoryAccess(mem);
		int version = header.getField(Version);
		InstructionDecoder decoder = new InstructionDecoder(config, version, memSeq);
		ZCharsAlphabet alphabet = new ZCharsAlphabet(config, version, header, mem);
		ZCharStream zCharStream = new ZCharsSeqMemUnpacker(memSeq);
		ZCharsToZSCIIConverterStream textConverter = new ZCharsToZSCIIConverterStream(config, version, header, mem, alphabet);
		alphabet.reset();
		textConverter.reset(zCharStream);
		memSeq.setAddress(header.getField(InitialPC15));//TODO V6+
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