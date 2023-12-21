package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.function.Function;

public class ZAssemblerUtils
{
	public static void checkBigintMaxByteCount(int maxBytes, BigInteger bigint, Function<BigInteger, String> errorMessage)
	{
		if(!hasBigintMaxByteCount(maxBytes, bigint))
			throw new IllegalArgumentException(errorMessage.apply(bigint));
	}

	public static boolean hasBigintMaxByteCount(int maxBytes, BigInteger bigint)
	{
		// We want to explicitly allow positive constants which would be negative if interpreted as two's complement.
		// We do this by checking bitLength against maxBytes*8, not maxBytes*8-1.
		// Note that this also allows negative constants which would be positive in two's complement.
		return bigint.bitLength() <= maxBytes * 8;
	}

	private ZAssemblerUtils()
	{}
}
