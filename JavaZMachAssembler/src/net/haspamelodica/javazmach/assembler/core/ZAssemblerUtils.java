package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ONE;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intVal;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.core.macrocontext.MacroContext;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedBinaryExpression;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralLiteral;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralValue;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReferenceIntegralOnly;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReferenceVariableOnly;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedUnaryExpression;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedVariable;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedVariableConstant;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.values.AlignmentValue;
import net.haspamelodica.javazmach.assembler.model.values.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.values.ByteSequenceElement;
import net.haspamelodica.javazmach.assembler.model.values.CString;
import net.haspamelodica.javazmach.assembler.model.values.CharLiteral;
import net.haspamelodica.javazmach.assembler.model.values.GlobalVariable;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.values.LocalVariable;
import net.haspamelodica.javazmach.assembler.model.values.NumberLiteral;
import net.haspamelodica.javazmach.assembler.model.values.SimpleAlignmentValue;
import net.haspamelodica.javazmach.assembler.model.values.StackPointer;
import net.haspamelodica.javazmach.assembler.model.values.Variable;
import net.haspamelodica.javazmach.assembler.model.values.ZString;
import net.haspamelodica.javazmach.assembler.model.values.zstrings.ZStringElement;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverterNoSpecialChars;
import net.haspamelodica.javazmach.core.text.ZCharsAlphabetTableDefault;
import net.haspamelodica.javazmach.core.text.ZSCIICharZCharConverter;

public class ZAssemblerUtils
{
	public static ResolvableCustomDefaultIntegralValue resolvableAlignmentValue(
			MacroContext macroContext, AlignmentValue alignment, int packedAlignment)
	{
		return resolvableAlignmentValue(macroContext, Optional.of(alignment), packedAlignment);
	}

	public static ResolvableCustomDefaultIntegralValue resolvableAlignmentValue(
			MacroContext macroContext, Optional<AlignmentValue> alignment, int packedAlignment)
	{
		return new ResolvableCustomDefaultIntegralValue(alignment
				.map(aa -> switch(aa)
				{
					case IntegralValue a -> intVal(macroContext.resolve(a));
					case SimpleAlignmentValue a -> intConst(packedAlignment);
				}).orElse(intConst(1)), ONE);
	}

	public static void alignToBytes(SequentialMemoryWriteAccess memSeq, ResolvableCustomDefaultIntegralValue alignment, DiagnosticHandler diagnosticHandler)
	{
		memSeq.alignToBytes(bigintIntChecked(31, alignment.resolvedValueOrDefault(),
				a -> "Alignment doesn't fit in int: " + a, diagnosticHandler));
	}

	public static Variable variableOrNull(ResolvedVariable variable, ValueReferenceResolver valueReferenceResolver)
	{
		return switch(variable)
		{
			case ResolvedVariableConstant s -> s.variable();
			case ResolvedLabelReferenceVariableOnly s -> s.macroContext().resolveLabelVariable(s.name(), valueReferenceResolver);
		};
	}

	public static BigInteger integralValueOrNull(ResolvedIntegralValue value, ValueReferenceResolver valueReferenceResolver)
	{
		return switch(value)
		{
			case ResolvedIntegralLiteral literal -> switch(literal.value())
			{
				case NumberLiteral l -> l.value();
				case CharLiteral l -> BigInteger.valueOf(l.value());
			};
			case ResolvedLabelReferenceIntegralOnly l -> l.macroContext().resolveLabelIntegral(l.name(), valueReferenceResolver);
			case ResolvedBinaryExpression expr ->
			{
				BigInteger lhs = integralValueOrNull(expr.lhs(), valueReferenceResolver);
				BigInteger rhs = integralValueOrNull(expr.rhs(), valueReferenceResolver);
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
			case ResolvedUnaryExpression expr ->
			{
				BigInteger operand = integralValueOrNull(expr.operand(), valueReferenceResolver);
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

	public static int varnumByte(Variable variable)
	{
		// It's not wanted to check if these are in range and emit a diagnostic otherwise:
		// All in-range variables have an ident,
		// so referring to variables with a variable literal is only necessary to deliberately access out-of-range variables,
		// which means that a diagnostic there would be counterproductive.
		return switch(variable)
		{
			case StackPointer var -> 0;
			case LocalVariable var ->
			{
				if(var.index() < 0 || var.index() > 0x0f)
					yield defaultError("Local variable out of range: " + var.index());
				yield var.index() + 0x1;
			}
			case GlobalVariable var ->
			{
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
	public static int wordLengthToZStringLength(int wordLength)
	{
		return wordLength * 3;
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
