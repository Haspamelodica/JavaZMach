package net.haspamelodica.javaz;

public class TestInstructionDecoder
{
	public static void main(String[] args)
	{
		print(5, 0x05, 0x02, 0x00, 0xd4);
		print(5, 0xd6, 0x2f, 0x03, 0xe8, 0x02, 0x00);
		print(5, 0x8f, 0x01, 0x56);
		print(5, 0xb2, 0x11, 0xaa, 0x46, 0x34, 0x16, 0x45, 0x9c, 0xa5);
	}
	private static void print(int version, int... bytes)
	{
		DecodedInstruction instr = new DecodedInstruction();
		SequentialMemoryAccess mem = new SequentialMemoryAccess(new Memory(bs(bytes)));
		mem.setAddress(0);
		InstructionDecoder decoder = new InstructionDecoder(new GlobalConfig(), version, mem);
		decoder.decode(instr);
		System.out.println(instr);
	}
	private static byte[] bs(int... bsAsInts)
	{
		byte[] bs = new byte[bsAsInts.length];
		for(int i = 0; i < bsAsInts.length; i ++)
			bs[i] = (byte) bsAsInts[i];
		return bs;
	}
}