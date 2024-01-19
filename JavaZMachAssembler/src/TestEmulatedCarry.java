
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

				short aLower = (short) (a >> 0);
				short aUpper = (short) (a >> 16);
				short bLower = (short) (b >> 0);
				short bUpper = (short) (b >> 16);

				aLower += bLower;
				if((short) (aLower + 0x8000) < (short) (bLower + 0x8000))
					// there was a carry
					bUpper ++;

				aUpper += bUpper;

				int actual = ((aUpper << 16) & 0xffff0000) | (aLower & 0xffff);

				if(expected != actual)
					throw new IllegalArgumentException("nope: %08x + %08x expected %08x, but was %08x".formatted(a, b, expected, actual));
			}
		}
	}
}
