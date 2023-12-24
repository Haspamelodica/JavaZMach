package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.model.BinaryExpression;
import net.haspamelodica.javazmach.assembler.model.CharLiteral;
import net.haspamelodica.javazmach.assembler.model.GlobalVariable;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.LabelReference;
import net.haspamelodica.javazmach.assembler.model.LocalVariable;
import net.haspamelodica.javazmach.assembler.model.NumberLiteral;
import net.haspamelodica.javazmach.assembler.model.StackPointer;
import net.haspamelodica.javazmach.assembler.model.UnaryExpression;
import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.assembler.model.ZString;
import net.haspamelodica.javazmach.assembler.model.ZStringElement;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverterNoSpecialChars;
import net.haspamelodica.javazmach.core.text.ZCharsAlphabetTableDefault;
import net.haspamelodica.javazmach.core.text.ZSCIICharZCharConverter;

public class ZAssemblerUtils
{
	public static BigInteger integralValue(IntegralValue value, LabelResolver labelResolver)
	{
		return switch(value)
		{
			case NumberLiteral literal -> literal.value();
			case CharLiteral literal -> BigInteger.valueOf(literal.value());
			case LabelReference labelRef -> BigInteger.valueOf(labelResolver.labelValue(labelRef.name()));
			case BinaryExpression expr ->
			{
				BigInteger lhs = integralValue(expr.lhs(), labelResolver);
				BigInteger rhs = integralValue(expr.rhs(), labelResolver);
				yield switch(expr.op())
				{
					case BITWISE_OR -> lhs.or(rhs);
					case BITWISE_XOR -> lhs.xor(rhs);
					case BITWISE_AND -> lhs.and(rhs);
					case LSHIFT -> lhs.shiftLeft(rhs.intValueExact());
					case RSHIFT -> lhs.shiftRight(rhs.intValueExact());
					case ADD -> lhs.add(rhs);
					case SUBTRACT -> lhs.subtract(rhs);
					case MULTIPLY -> lhs.multiply(rhs);
					case DIVIDE -> lhs.divide(rhs);
					case MODULO -> lhs.mod(rhs);
				};
			}
			case UnaryExpression expr ->
			{
				BigInteger operand = integralValue(expr.operand(), labelResolver);
				yield switch(expr.op())
				{
					case NEGATE -> operand.negate();
					case BITWISE_NOT -> operand.not();
				};
			}
		};
	}

	public static int varnumByteAndUpdateRoutine(Variable variable)
	{
		return switch(variable)
		{
			case StackPointer var -> 0;
			case LocalVariable var ->
			{
				if(var.index() < 0 || var.index() > 0x0f)
					throw new IllegalArgumentException("Local variable out of range: " + var.index());
				System.err.println("WARNING: local variable indices not yet checked against routine");
				//TODO check against current routine once those are implemented
				//TODO update routine once implemented
				yield var.index() + 0x1;
			}
			case GlobalVariable var ->
			{
				//TODO check against global variable table length
				if(var.index() < 0 || var.index() > 0xef)
					throw new IllegalArgumentException("Global variable out of range: " + var.index());
				yield var.index() + 0x10;
			}
		};
	}

	public static <R> R throwShortOverrideButNotShort(int branchOffsetEncodedOrZero)
	{
		throw new IllegalArgumentException("Branch target length is overridden to be short, "
				+ "but actual encoded value can't be assembled in short form: " + branchOffsetEncodedOrZero);
	}

	public static void appendZString(SequentialMemoryWriteAccess target, ZString text, int version)
	{
		List<Byte> zchars = toZChars(text, version);
		while(zchars.size() % 3 != 0)
			zchars.add((byte) 5);
		for(int i = 0; i < zchars.size(); i += 3)
			target.writeNextWord(0
					// at end: bit 15; 0 means no, 1 means yes
					| ((i == zchars.size() - 3 ? 1 : 0) << 15)
					// Z-char 1: bits 14-10
					| ((zchars.get(i + 0) & 0x1f) << 10)
					// Z-char 2: bits 9-5
					| ((zchars.get(i + 1) & 0x1f) << 5)
					// Z-char 3: bits 4-0
					| ((zchars.get(i + 2) & 0x1f) << 0));
	}

	private static List<Byte> toZChars(ZString text, int version)
	{
		// TODO what about custom alphabets?
		ZSCIICharZCharConverter converter = new ZSCIICharZCharConverter(version, new ZCharsAlphabetTableDefault(version));

		List<Byte> result = new ArrayList<>();
		for(ZStringElement element : text.elements())
		{
			// TODO abbreviation handling, once implemented
			element
					.string()
					.codePoints()
					.peek(cp ->
					{
						if(cp == '\r')
							System.err.println("WARNING: \\r in ZSCII text will be ignored; use \\n for linebreaks instead.");
					})
					.filter(cp -> cp != '\r')
					.map(UnicodeZSCIIConverterNoSpecialChars::unicodeToZsciiNoCR)
					.forEach(zsciiChar -> converter.translateZSCIIToZChars(zsciiChar, result::add));
		}

		return result;
	}

	public static String versionRangeString(int minVersion, int maxVersion)
	{
		return "V" + minVersion + (maxVersion <= 0 ? "+" : maxVersion != minVersion ? "-" + maxVersion : "");
	}

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
