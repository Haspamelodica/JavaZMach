package net.haspamelodica.javaz;

public class TestInstructionDecoder
{
	public static void main(String[] args)
	{
		DecodedInstruction decodedInstruction = new DecodedInstruction();
		InstructionDecoder decoder = new InstructionDecoder(new GlobalConfig());

		System.out.println(decoder.decode(new Memory(bs(0x05, 0x02, 0x00, 0xd4)), 0, 5, decodedInstruction));
		System.out.println(decodedInstruction);

		System.out.println(decoder.decode(new Memory(bs(0xd6, 0x2f, 0x03, 0xe8, 0x02, 0x00)), 0, 1, decodedInstruction));
		System.out.println(decodedInstruction);

		System.out.println(decoder.decode(new Memory(bs(0x8f, 0x01, 0x56)), 0, 5, decodedInstruction));
		System.out.println(decodedInstruction);
	}
	private static byte[] bs(int... bsAsInts)
	{
		byte[] bs = new byte[bsAsInts.length];
		for(int i = 0; i < bsAsInts.length; i ++)
			bs[i] = (byte) bsAsInts[i];
		return bs;
	}
}