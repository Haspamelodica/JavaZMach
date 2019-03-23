package net.haspamelodica.javaz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DecompileBasic
{
	public static void main(String[] args) throws IOException
	{
		Memory mem = new Memory(Files.readAllBytes(Paths.get("ZORK1.z3")));
		HeaderParser header = new HeaderParser(mem);
		SequentialMemoryAccess memSeq = new SequentialMemoryAccess(mem);
		InstructionDecoder decoder = new InstructionDecoder(new GlobalConfig(), 3, memSeq);
		memSeq.setAddress(header.getField(HeaderParser.InitialPCLoc));
		DecodedInstruction instr = new DecodedInstruction();
		do
		{
			System.out.print("pc ");
			System.out.printf("%06x", memSeq.getAddress());
			decoder.decode(instr);
			System.out.print(": ");
			System.out.println(instr);
		} while(instr.opcode != Opcode._unknown_instr);
	}
}