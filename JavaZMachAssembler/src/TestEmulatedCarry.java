
public class TestEmulatedCarry
{
	public static void main(String[] args)
	{
		for(int a = 0; a < 0x20000; a ++)
		{
			if(a % 0x100 == 0)
				System.out.println("%04x".formatted(a));
			for(int b = 0; b < 0x20000; b ++)
			{
				int expected = a + b;

				byte a0 = (byte) (a >> 0x00);
				byte b0 = (byte) (b >> 0x00);
				byte a1 = (byte) (a >> 0x08);
				byte b1 = (byte) (b >> 0x08);
				byte a2 = (byte) (a >> 0x10);
				byte b2 = (byte) (b >> 0x10);
				byte a3 = (byte) (a >> 0x18);
				byte b3 = (byte) (b >> 0x18);

				boolean b1_overflow = false;
				a0 += b0;
				if((byte) (a0 + 0x80) < (byte) (b0 + 0x80))
				{
					// there was a carry
					b1 ++;
					if(b1 == 0x0000)
						b1_overflow = true;
				}

				if(!b1_overflow)
					a1 += b1;
				boolean b2_overflow = false;
				if(b1_overflow || (byte) (a1 + 0x80) < (byte) (b1 + 0x80))
				{
					// there was a carry
					b2 ++;
					if(b2 == 0x0000)
						b2_overflow = true;
				}

				if(!b2_overflow)
					a2 += b2;
				boolean b3_overflow = false;
				if(b2_overflow || (byte) (a2 + 0x80) < (byte) (b2 + 0x80))
				{
					// there was a carry
					b3 ++;
					if(b3 == 0x0000)
						b3_overflow = true;
				}

				if(!b3_overflow)
					a3 += b3;
				if(b3_overflow || (byte) (a3 + 0x80) < (byte) (b3 + 0x80))
				{
					// there was a carry
					System.out.println("carry out of int");
				}

				int actual = 0
						| ((a0 & 0xff) << 0x00)
						| ((a1 & 0xff) << 0x08)
						| ((a2 & 0xff) << 0x10)
						| ((a3 & 0xff) << 0x18);

				if(expected != actual)
					throw new IllegalArgumentException("incorrect for %08x + %08x: expected %08x, but was %08x".formatted(a, b, expected, actual));
			}
		}
	}
}
