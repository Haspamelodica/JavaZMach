package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.model.BinaryExpression;
import net.haspamelodica.javazmach.assembler.model.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.ByteSequenceElement;
import net.haspamelodica.javazmach.assembler.model.CString;
import net.haspamelodica.javazmach.assembler.model.CharLiteral;
import net.haspamelodica.javazmach.assembler.model.GlobalVariable;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.LabelReference;
import net.haspamelodica.javazmach.assembler.model.LocalVariable;
import net.haspamelodica.javazmach.assembler.model.MacroParamRef;
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
	public static BigInteger integralValueOrNull(MacroContext macroContext, IntegralValue value, ValueReferenceResolver valueReferenceResolver)
	{
		return switch(value)
		{
			case NumberLiteral literal -> literal.value();
			case CharLiteral literal -> BigInteger.valueOf(literal.value());
			case LabelReference labelRef -> macroContext.resolveLabelRef(labelRef.name(), valueReferenceResolver);
			case MacroParamRef macroParamRef -> macroContext.resolveIntegralValue(macroParamRef, valueReferenceResolver);
			case BinaryExpression expr ->
			{
				BigInteger lhs = integralValueOrNull(macroContext, expr.lhs(), valueReferenceResolver);
				BigInteger rhs = integralValueOrNull(macroContext, expr.rhs(), valueReferenceResolver);
				if(lhs == null || rhs == null)
					yield null;
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
				BigInteger operand = integralValueOrNull(macroContext, expr.operand(), valueReferenceResolver);
				if(operand == null)
					yield null;
				yield switch(expr.op())
				{
					case NEGATE -> operand.negate();
					case BITWISE_NOT -> operand.not();
				};
			}
		};
	}

	//TODO: this should accept a diagnostic handler
	public static byte[] materializeByteSequence(ByteSequence byteSequence, int version, Function<String, String> errorMessage)
	{

		int length = byteSequence
				.elements()
				.stream()
				.mapToInt(e -> switch(e)
				{
					case NumberLiteral elementInteger -> 1;
					case CString elementString -> elementString.value().length();
					// This is ugly, computing this twice per zString...
					case ZString elementString -> ZAssemblerUtils.zStringWordLength(ZAssemblerUtils.toZChars(elementString, version)) * 2;
					case CharLiteral elementChar -> 1;
				})
				.sum();
		byte[] value = new byte[length];
		int i = 0;
		for(ByteSequenceElement elementUncasted : byteSequence.elements())
		{
			switch(elementUncasted)
			{
				case NumberLiteral element -> value[i ++] = (byte) bigintIntChecked(8,
						// We can use the default diagnostic handler here - we don't have a LocationResolver anyway, so errors can't change
						element.value(), bigint -> errorMessage.apply("byte literal out of range: " + bigint), DiagnosticHandler.defaultHandler());
				case CString element ->
				{
					System.arraycopy(element.value().getBytes(StandardCharsets.US_ASCII), 0, value, i, element.value().length());
					i += element.value().length();
				}
				case ZString element ->
				{
					NoRangeCheckMemory mem = new NoRangeCheckMemory();
					SequentialMemoryWriteAccess memSeq = new SequentialMemoryWriteAccess(mem);
					ZAssemblerUtils.appendZString(memSeq, element, version);
					System.arraycopy(mem.data(), 0, value, i, mem.currentSize());
					i += mem.currentSize();
				}
				case CharLiteral element ->
				{
					if((element.value() & ~0x7f) != 0)
						defaultError(errorMessage.apply("char literal out of range (not ASCII): " + element.value()));
					value[i ++] = (byte) element.value();
				}
			};
		}
		return value;
	}

	public static int varnumByteAndUpdateRoutine(Variable variable)
	{
		return switch(variable)
		{
			case StackPointer var -> 0;
			case LocalVariable var ->
			{
				if(var.index() < 0 || var.index() > 0x0f)
					yield defaultError("Local variable out of range: " + var.index());
				defaultWarning("local variable indices not yet checked against routine");
				//TODO check against current routine once those are implemented
				//TODO update routine once implemented
				yield var.index() + 0x1;
			}
			case GlobalVariable var ->
			{
				//TODO check against global variable table length
				if(var.index() < 0 || var.index() > 0xef)
					yield defaultError("Global variable out of range: " + var.index());
				yield var.index() + 0x10;
			}
		};
	}

	public static void appendZString(SequentialMemoryWriteAccess target, ZString text, int version)
	{
		appendZChars(target, toZChars(text, version));
	}

	public static void appendZChars(SequentialMemoryWriteAccess target, List<Byte> zchars)
	{
		if(zchars.size() % 3 != 0)
		{
			zchars = new ArrayList<>(zchars);
			do
				zchars.add((byte) 5);
			while(zchars.size() % 3 != 0);
		}

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

	public static int zStringWordLength(List<Byte> zchars)
	{
		// +2 to round up
		return (zchars.size() + 2) / 3;
	}

	public static List<Byte> toZChars(ZString text, int version)
	{
		// TODO what about custom alphabets?
		ZSCIICharZCharConverter converter = new ZSCIICharZCharConverter(version, new ZCharsAlphabetTableDefault(version));

		List<Byte> zchars = new ArrayList<>();
		for(ZStringElement element : text.elements())
		{
			// TODO abbreviation handling, once implemented
			element
					.string()
					.codePoints()
					.peek(cp ->
					{
						if(cp == '\r')
							defaultWarning("\\r in ZSCII text will be ignored; use \\n for linebreaks instead.");
					})
					.filter(cp -> cp != '\r')
					.map(UnicodeZSCIIConverterNoSpecialChars::unicodeToZsciiNoCR)
					.forEach(zsciiChar -> converter.translateZSCIIToZChars(zsciiChar, zchars::add));
		}

		return List.copyOf(zchars);
	}

	public static String versionRangeString(int minVersion, int maxVersion)
	{
		return "V" + minVersion + (maxVersion <= 0 ? "+" : maxVersion != minVersion ? "-" + maxVersion : "");
	}

	public static int bigintIntChecked(int maxBits, BigInteger bigint, Function<BigInteger, String> errorMessage, DiagnosticHandler diagnosticHandler)
	{
		checkBigintMaxBitCount(maxBits, bigint, errorMessage, diagnosticHandler);
		return bigint.intValue() & (-1 >>> (32 - maxBits));
	}

	public static byte[] bigintBytesChecked(int maxBits, BigInteger bigint, Function<BigInteger, String> errorMessage, DiagnosticHandler diagnosticHandler)
	{
		if(!hasBigintMaxBitCount(maxBits, bigint))
		{
			diagnosticHandler.error(errorMessage.apply(bigint));
		}
		int requiredLen = (maxBits + 7) / 8;
		byte bytes[] = bigint.toByteArray();
		if(requiredLen < bytes.length)
		{
			// This case can occur if either the unsigned BigInteger
			// truly has more bits than maxBits, or if the sign bit
			// requires an additional byte.
			// This is in line with the definition of hasBigIntMaxBitCount
			return Arrays.copyOfRange(bytes, bytes.length - requiredLen, bytes.length);
		} else
		{
			return bytes;
		}
	}

	public static void checkBigintMaxBitCount(int maxBits, BigInteger bigint,
			Function<BigInteger, String> errorMessage, DiagnosticHandler diagnosticHandler)
	{
		if(!hasBigintMaxBitCount(maxBits, bigint))
			diagnosticHandler.error(errorMessage.apply(bigint));
	}

	public static boolean hasBigintMaxBitCountAndIsPositive(int maxBits, BigInteger bigint)
	{
		return bigint.signum() >= 0 && hasBigintMaxBitCount(maxBits, bigint);
	}

	public static boolean hasBigintMaxBitCount(int maxBits, BigInteger bigint)
	{
		// We want to explicitly allow positive values which would be negative if interpreted as two's complement.
		// We do this by checking bitLength against maxBits, not maxBits-1.
		// Note that this also allows negative values which would be positive in two's complement.
		return bigint.bitLength() <= maxBits;
	}

	private ZAssemblerUtils()
	{}
}
