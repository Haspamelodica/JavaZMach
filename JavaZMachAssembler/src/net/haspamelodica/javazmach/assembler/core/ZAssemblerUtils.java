package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.function.Function;

public class ZAssemblerUtils
{
	public static int bigintIntChecked(int maxBits, BigInteger bigint, Function<BigInteger, String> errorMessage)
	{
		checkBigintMaxBitCount(maxBits, bigint, errorMessage);
		return bigint.intValue() & ((1 << maxBits) - 1);
	}

	public static byte[] bigintBytesChecked(int maxBits, BigInteger bigint, Function<BigInteger, String> errorMessage)
	{
		checkBigintMaxBitCount(maxBits, bigint, errorMessage);
		return bigint.toByteArray();
	}

	public static void checkBigintMaxBitCount(int maxBits, BigInteger bigint, Function<BigInteger, String> errorMessage)
	{
		if(!hasBigintMaxBitCount(maxBits, bigint))
			throw new IllegalArgumentException(errorMessage.apply(bigint));
	}

	public static boolean hasBigintMaxBitCountAndIsPositive(int maxBits, BigInteger bigint)
	{
		return bigint.signum() >= 0 && hasBigintMaxBitCount(maxBits, bigint);
	}

	public static boolean hasBigintMaxBitCount(int maxBits, BigInteger bigint)
	{
		// We want to explicitly allow positive constants which would be negative if interpreted as two's complement.
		// We do this by checking bitLength against maxBits, not maxBits-1.
		// Note that this also allows negative constants which would be positive in two's complement.
		return bigint.bitLength() <= maxBits;
	}

	private ZAssemblerUtils()
	{}
}
