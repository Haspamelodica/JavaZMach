package net.haspamelodica.javaz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.model.HeaderParser;
import net.haspamelodica.javaz.model.instructions.DecodedInstruction;
import net.haspamelodica.javaz.model.instructions.InstructionDecoder;
import net.haspamelodica.javaz.model.instructions.Opcode;
import net.haspamelodica.javaz.model.memory.SequentialMemoryAccess;
import net.haspamelodica.javaz.model.memory.WritableFixedSizeMemory;
import net.haspamelodica.javaz.model.memory.WritableMemory;
import net.haspamelodica.javaz.model.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javaz.model.text.ZCharsToZSCIIUnpacker;
import net.haspamelodica.javaz.model.text.ZSCIICharStreamReceiver;

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
		ZCharsToZSCIIUnpacker textUnpacker = new ZCharsToZSCIIUnpacker(config, version, header, mem, new ZCharsSeqMemUnpacker(memSeq));
		textUnpacker.reset();
		ZSCIICharStreamReceiver textReceiver = zsciiChar -> System.out.print((char) zsciiChar);
		memSeq.setAddress(header.getField(HeaderParser.InitialPCLoc));
		memSeq.setAddress(0x535E);//address of Overview example
		DecodedInstruction instr = new DecodedInstruction();
		do
		{
			System.out.print("pc ");
			System.out.printf("%06x", memSeq.getAddress());
			decoder.decode(instr);
			System.out.print(": ");
			System.out.print(instr);
			if(instr.opcode.isTextOpcode)
			{
				System.out.print(" text: \"");
				textUnpacker.decode(textReceiver);
				System.out.print('"');
			}
			System.out.println();
		} while(instr.opcode != Opcode._unknown_instr);
	}
}