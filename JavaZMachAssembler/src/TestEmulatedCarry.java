
public class TestEmulatedCarry
{
	public static void main(String[] args)
	{
		for(int a = 0; a < 0x2000; a ++)
		{
			if(a % 0x400 == 0)
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

				int actualVars = emulatedCarryVars(a0, b0, a1, b1, a2, b2, a3, b3);
				if(expected != actualVars)
					throw new IllegalArgumentException("vars incorrect for %08x + %08x: expected %08x, but was %08x".formatted(a, b, expected, actualVars));

				int actualConst = emulatedCarryConst(a0, b0, a1, b1, a2, b2, a3, b3);
				if(expected != actualConst)
					throw new IllegalArgumentException("const incorrect for %08x + %08x: expected %08x, but was %08x".formatted(a, b, expected, actualConst));
			}
		}
	}

	public static int emulatedCarryVars(byte a0, byte b0, byte a1, byte b1, byte a2, byte b2, byte a3, byte b3)
	{
		// These simulate conditional jumps:
		// "aN_carry = true;" is triggering the jump,
		// "if(aN_carry)" is the jump target,
		// and all code between the trigger and the target is guarded with "if(!aN_carry)".
		// Also, there will be an implicit jump from just before the "if(aN_carry)" to after it.
		boolean a0_carry = false;
		boolean a1_carry = false;
		boolean a2_carry = false;
		boolean a3_carry = false;

		if(!a0_carry)
		{
			b0 += 0x80;
			a0 += b0;
			a0_carry = a0 < b0;
		}
		if(a0_carry)
		{
			// there was a carry
			b1 ++;
			if(b1 == 0x00)
			{
				a1 += 0x80;
				a1_carry = true;
			}
		}

		if(!a1_carry)
		{
			b1 += 0x80;
			a1 += b1;
			a1_carry = a1 < b1;
		}
		if(a1_carry)
		{
			// there was a carry
			b2 ++;
			if(b2 == 0x00)
			{
				a2 += 0x80;
				a2_carry = true;
			}
		}

		if(!a2_carry)
		{
			b2 += 0x80;
			a2 += b2;
			a2_carry = a2 < b2;
		}
		if(a2_carry)
		{
			// there was a carry
			b3 ++;
			if(b3 == 0x00)
			{
				a3 += 0x80;
				a3_carry = true;
			}
		}

		if(!a3_carry)
		{
			b3 += 0x80;
			a3 += b3;
			a3_carry = a3 < b3;
		}
		if(a3_carry)
		{
			// there was a carry
			System.out.println("carry out of int");
		}
		a0 += 0x80;
		a1 += 0x80;
		a2 += 0x80;
		a3 += 0x80;

		int actual = 0
				| ((a0 & 0xff) << 0x00)
				| ((a1 & 0xff) << 0x08)
				| ((a2 & 0xff) << 0x10)
				| ((a3 & 0xff) << 0x18);
		return actual;
	}

	public static int emulatedCarryConst(byte a0, final byte b0, byte a1, final byte b1, byte a2, final byte b2, byte a3, byte b3)
	{
		// These simulate conditional jumps:
		// "aN_carry = true;" is triggering the jump,
		// "if(aN_carry)" is the jump target,
		// and all code between the trigger and the target is guarded with "if(!aN_carry)".
		// Also, there will be an implicit jump from just before the "if(aN_carry)" to after it.
		boolean a0_carry = false;
		boolean a1_carry = false;
		boolean a2_carry = false;
		boolean a3_carry = false;
		boolean a0_justcheck = false;
		boolean a1_justcheck = false;
		boolean a2_justcheck = false;
		boolean a3_justcheck = false;

		if(!a0_justcheck && !a0_carry)
			a0 += b0 + 0x80;
		if(!a0_carry)
			a0_carry = a0 < (byte) (b0 + 0x80);
		if(a0_carry)
		{
			// there was a carry
			a1 += b1 + 0x81;
			if(b1 == (byte) 0xff)
				a1_carry = true;
			a1_justcheck = true;
		}

		if(!a1_justcheck && !a1_carry)
			a1 += b1 + 0x80;
		if(!a1_carry)
			a1_carry = a1 < (byte) (b1 + 0x80);
		if(a1_carry)
		{
			// there was a carry
			a2 += b2 + 0x81;
			if(b2 == (byte) 0xff)
				a2_carry = true;
			a2_justcheck = true;
		}

		if(!a2_justcheck && !a2_carry)
			a2 += b2 + 0x80;
		if(!a2_carry)
			a2_carry = a2 < (byte) (b2 + 0x80);
		if(a2_carry)
		{
			// there was a carry
			a3 += b3 + 0x81;
			if(b3 == (byte) 0xff)
				a3_carry = true;
			a3_justcheck = true;
		}

		if(!a3_justcheck && !a3_carry)
			a3 += b3 + 0x80;
		if(!a3_carry)
			a3_carry = a3 < (byte) (b3 + 0x80);
		if(a3_carry)
		{
			// there was a carry
			System.out.println("carry out of int");
		}
		a0 += 0x80;
		a1 += 0x80;
		a2 += 0x80;
		a3 += 0x80;

		int actual = 0
				| ((a0 & 0xff) << 0x00)
				| ((a1 & 0xff) << 0x08)
				| ((a2 & 0xff) << 0x10)
				| ((a3 & 0xff) << 0x18);
		return actual;
	}
}
