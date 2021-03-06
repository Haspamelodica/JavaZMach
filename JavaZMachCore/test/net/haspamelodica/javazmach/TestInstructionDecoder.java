package net.haspamelodica.javazmach;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.haspamelodica.javazmach.core.instructions.DecodedInstruction;
import net.haspamelodica.javazmach.core.instructions.InstructionDecoder;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javazmach.core.memory.StaticArrayBackedMemory;

public class TestInstructionDecoder
{
	@Test
	public void testBasic()
	{
		print(5, 0x05, 0x02, 0x00, 0xd4);
		print(5, 0xb2, 0x11, 0xaa, 0x46, 0x34, 0x16, 0x45, 0x9c, 0xa5);
		print(5, 0xd6, 0x2f, 0x03, 0xe8, 0x02, 0x00);
		print(5, 0x8f, 0x01, 0x56);
	}
	private static void print(int version, int... bytes)
	{
		DecodedInstruction instr = new DecodedInstruction();
		SequentialMemoryAccess mem = new SequentialMemoryAccess(new StaticArrayBackedMemory(bs(bytes)));
		mem.setAddress(0);
		InstructionDecoder decoder = new InstructionDecoder(new GlobalConfig(), version, mem);
		decoder.decode(instr);
		System.out.println(instr);
		assertEquals(bytes.length, mem.getAddress());
	}
	private static byte[] bs(int... bsAsInts)
	{
		byte[] bs = new byte[bsAsInts.length];
		for(int i = 0; i < bsAsInts.length; i ++)
			bs[i] = (byte) bsAsInts[i];
		return bs;
	}
}