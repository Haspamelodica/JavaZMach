package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.NoRangeCheckMemory;
import net.haspamelodica.javazmach.assembler.model.ConstantByteSequence;
import net.haspamelodica.javazmach.assembler.model.ConstantByteSequenceElement;
import net.haspamelodica.javazmach.assembler.model.ConstantChar;
import net.haspamelodica.javazmach.assembler.model.ConstantInteger;
import net.haspamelodica.javazmach.assembler.model.ConstantString;
import net.haspamelodica.javazmach.assembler.model.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.Label;
import net.haspamelodica.javazmach.assembler.model.LabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.instructions.Opcode;

public class ZAssembler
{
	private static final Set<HeaderField> AUTO_FIELDS = Set.of(HeaderField.FileLength);

	private final int					version;
	private final Map<String, Opcode>	opcodesByNameLowercase;

	private final NoRangeCheckMemory	header;
	private final List<Reference>		references;

	private final Set<HeaderField>	setFields;
	private final Set<HeaderField>	partiallySetBitfields;

	private final NoRangeCheckMemory	code;
	private final Map<String, Integer>	codeLabelRelAddrs;

	public ZAssembler(int version)
	{
		this.version = version;
		this.opcodesByNameLowercase = Arrays
				.stream(Opcode.values())
				.filter(o -> o != Opcode._unknown_instr)
				.filter(o -> version >= o.minVersion)
				.filter(o -> version <= o.maxVersion || o.maxVersion <= 0)
				// careful: don't use method "name()", but member "name".
				.collect(Collectors.toUnmodifiableMap(o -> o.name.toLowerCase(), o -> o));

		this.header = new NoRangeCheckMemory();
		this.references = new ArrayList<>();
		this.code = new NoRangeCheckMemory();
		this.setFields = new HashSet<>();
		this.partiallySetBitfields = new HashSet<>();
		this.codeLabelRelAddrs = new HashMap<>();
	}

	public void add(ZAssemblerFile file)
	{
		if(file.version().isPresent() && file.version().getAsInt() != version)
			throw new IllegalArgumentException("Version mismatch");

		add(file.entries());
	}

	public void add(List<ZAssemblerFileEntry> entries)
	{
		for(ZAssemblerFileEntry entry : entries)
			add(entry);
	}

	public void add(ZAssemblerFileEntry entry)
	{
		switch(entry)
		{
			case HeaderEntry headerEntry -> add(headerEntry);
			case LabelDeclaration labelDeclaration -> add(labelDeclaration);
			case ZAssemblerInstruction instruction -> add(instruction);
		}
	}

	public void add(HeaderEntry headerEntry)
	{
		HeaderField field;
		try
		{
			field = HeaderField.valueOf(headerEntry.name());
		} catch(IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Unknown header field: " + headerEntry.name());
		}

		boolean isBitfieldEntry = field.bitfield != null;
		if(isBitfieldEntry)
			partiallySetBitfields.add(field.bitfield);

		if(version < field.minVersion || (field.maxVersion > 0 && version > field.maxVersion))
			throw new IllegalArgumentException("Field " + field + " does not exist in version " + version
					+ "; only V" + field.minVersion +
					(field.maxVersion <= 0 ? "+" : field.maxVersion != field.minVersion ? "-" + field.maxVersion : ""));
		if(!setFields.add(field))
			System.err.println("WARNING: Field " + field + " set twice - old value will be overwritten");
		if(field.isBitfield && partiallySetBitfields.contains(field))
			System.err.println("WARNING: Bitfield " + field
					+ " is set after some bitfield entries have been set - old bitfield entry values will be overwritten");
		if(field.isRst)
			System.err.println("INFO: Field " + field + " is Rst - will usually be overwritten by interpreter on startup");

		if(AUTO_FIELDS.contains(field))
			System.err.println("INFO: Automatically computed value of " + field + " is overwritten with explicit value");

		switch(headerEntry.value())
		{
			case ConstantInteger constant ->
			{
				if(!isBitfieldEntry)
				{
					checkHeaderBigintMaxByteCount(field.len, constant.value(), bigint -> "constant out of range: "
							+ bigint + " for field " + field);
					byte[] valueBytes = constant.value().toByteArray();
					int padding = field.len - valueBytes.length;
					if(padding != 0)
					{
						byte[] valueBytesOrig = valueBytes;
						valueBytes = new byte[field.len];
						System.arraycopy(valueBytesOrig, 0, valueBytes, padding, valueBytesOrig.length);
						if(constant.value().signum() < 0)
							Arrays.fill(valueBytes, 0, padding, (byte) -1);
					}
					HeaderParser.setFieldUncheckedBytes(header, field, valueBytes, 0, field.len);
				} else
				{
					if(constant.value().signum() != 0 && !constant.value().equals(BigInteger.ONE))
						throw new IllegalArgumentException("Value of bitfield entry is neither 0 nor 1: field "
								+ field + ", value " + constant.value());

					HeaderParser.setFieldUnchecked(header, field, constant.value().testBit(0) ? 1 : 0);
				}
			}
			case ConstantByteSequence constant ->
			{
				int length = constant
						.entries()
						.stream()
						.mapToInt(e -> switch(e)
						{
							case ConstantInteger elementInteger -> 1;
							case ConstantString elementString -> elementString.value().length();
							case ConstantChar elementChar -> 1;
						})
						.sum();
				byte[] value = new byte[length];
				int i = 0;
				for(ConstantByteSequenceElement elementUncasted : constant.entries())
					switch(elementUncasted)
					{
						case ConstantInteger element ->
						{
							checkHeaderBigintMaxByteCount(1, element.value(), bigint -> "byte constant out of range: "
									+ bigint + " for field " + field);
							value[i ++] = element.value().byteValue();
						}
						case ConstantString element ->
						{
							System.arraycopy(element.value().getBytes(StandardCharsets.US_ASCII), 0, value, 0, element.value().length());
							i += element.value().length();
						}
						case ConstantChar element ->
						{
							if((element.value() & ~0x7f) != 0)
								throw new IllegalArgumentException("char constant out of range (not ASCII): " + element.value()
										+ " for field " + field);
							value[i ++] = (byte) element.value();
						}
					};

				if(isBitfieldEntry)
					throw new IllegalArgumentException("Setting a bitfield entry to a byte sequence "
							+ "(not a single integer literal) is nonsensical: " + field);

				if(field.len > value.length)
					System.err.println("WARNING: Byte sequence value for field " + field + " is too short ("
							+ value.length + "<" + field.len + "); will be padded with nullbytes");
				else if(field.len < value.length)
					throw new IllegalArgumentException("Byte sequence value for field " + field + " is too long: "
							+ value.length + ">" + field.len);

				HeaderParser.setFieldUncheckedBytes(header, field, value);
			}
			case Label label ->
			{
				if(isBitfieldEntry)
					throw new IllegalArgumentException("Setting a bitfield entry to a label is nonsensical");
				references.add(new Reference(new HeaderFieldReferenceSource(field), new CodeLabelReferenceTarget(label.name())));
			}
		}
	}

	public void checkHeaderBigintMaxByteCount(int maxBytes, BigInteger bigint, Function<BigInteger, String> errorMessage)
	{
		// Not using xyzValueExact: we want to explicitly allow positive constants which would be negative if interpreted as two's complement.
		// We do this by checking bitLength against maxBytes*8, not maxBytes*8-1.
		// Note that this also allows negative constants below 0x80.
		if(bigint.bitLength() > maxBytes * 8)
			throw new IllegalArgumentException(errorMessage.apply(bigint));
	}

	public void add(LabelDeclaration labelDeclaration)
	{
		addCodeLabelHere(labelDeclaration.name());
	}

	public void add(ZAssemblerInstruction instruction)
	{
		Opcode opcode = opcodesByNameLowercase.get(instruction.name().toLowerCase());
		if(opcode == null)
			// shouldn't really matter - the grammar knows which opcodes there are.
			// Still, better safe than sorry - ZAssembler might theoretically be used without ZAssemblerParser,
			// and the way instructions are parsed also might change later.
			throw new IllegalArgumentException("Opcode " + instruction.name() + " unknown");

		//TODO
		System.out.println(instruction);
	}

	public byte[] assemble()
	{
		preAssembleHeaderSection();
		preAssembleCodeSection();

		// compute where each section will end up
		int headerStart = 0;
		int headerEnd = headerStart + header.currentSize();
		int codeStart = headerEnd;
		int codeEnd = codeStart + code.currentSize();
		int storyfileSize = codeEnd;

		references.forEach(ref ->
		{
			int value = switch(ref.referent())
			{
				case SimpleReferenceTarget referent -> switch(referent)
				{
					case FileLengthForHeader -> switch(version)
					{
						case 1, 2, 3 -> storyfileSize / 2;
						case 4, 5 -> storyfileSize / 4;
						case 6, 7, 8 -> storyfileSize / 8;
						default -> throw new IllegalStateException("Unknown version: " + version + "; don't know how file length is stored");
					};
				};
				case CodeLabelReferenceTarget referent -> codeLabelRelAddrs.get(referent.label()) + codeStart;
			};

			switch(ref.referrer())
			{
				case HeaderFieldReferenceSource referrer -> HeaderParser.setFieldUnchecked(header, referrer.field(), value);
			}
		});

		byte[] result = new byte[storyfileSize];
		System.arraycopy(header.data(), 0, result, headerStart, header.currentSize());
		System.arraycopy(code.data(), 0, result, headerStart, code.currentSize());
		return result;
	}

	private void preAssembleHeaderSection()
	{
		for(HeaderField automaticField : AUTO_FIELDS)
			if(!setFields.contains(automaticField))
				switch(automaticField)
				{
					case FileLength -> references.add(new Reference(new HeaderFieldReferenceSource(HeaderField.FileLength), SimpleReferenceTarget.FileLengthForHeader));
					default -> throw new IllegalStateException("Field " + automaticField
							+ " is marked as auto, but is not!? This is an assembler bug.");
				}

		// ensure header is at least 0x40 / 64byte in size (by padding with nullbytes)
		if(header.currentSize() < 0x40)
			header.writeByte(0x3f, 0);

		List<HeaderField> unsetHeaderFields = Arrays.stream(HeaderField.values())
				.filter(f -> version >= f.minVersion)
				.filter(f -> f.maxVersion <= 0 || version <= f.maxVersion)
				.filter(f -> !f.isRst)
				.filter(f -> !AUTO_FIELDS.contains(f))
				.filter(f -> !setFields.contains(f))
				.filter(f -> f.bitfield == null || !setFields.contains(f.bitfield))
				.toList();

		Map<HeaderField, String> unsetBitfieldEntries = unsetHeaderFields
				.stream()
				.filter(f -> f.isBitfield)
				.collect(Collectors.toMap(f -> f, bitfield -> unsetHeaderFields
						.stream()
						.filter(f -> f.bitfield == bitfield)
						.map(HeaderField::name)
						.collect(Collectors.joining(","))));

		String unsetHeaderFieldsStr = unsetHeaderFields.stream()
				.filter(f -> f.bitfield == null)
				.filter(f -> !f.isBitfield || !unsetBitfieldEntries.get(f).isEmpty())
				.map(f -> f.isBitfield ? f.name() + "[" + unsetBitfieldEntries.get(f) + "]" : f.name())
				.collect(Collectors.joining(", "));

		if(!unsetHeaderFieldsStr.isEmpty())
			System.err.println("INFO: The following non-Rst header fields have no explicit value and will default to 0: " + unsetHeaderFieldsStr);
	}

	private void preAssembleCodeSection()
	{
		for(Reference ref : references)
			if(ref.referent() instanceof CodeLabelReferenceTarget referent)
				if(!codeLabelRelAddrs.containsKey(referent.label()))
					throw new IllegalArgumentException("Undefined label: " + referent.label());
	}

	private void addCodeLabelHere(String label)
	{
		addCodeLabel(label, code.currentSize());
	}

	private void addCodeLabel(String label, int relativeAddress)
	{
		Integer old = codeLabelRelAddrs.put(label, relativeAddress);
		if(old != null)
			throw new IllegalArgumentException("Duplicate label: " + label + " (relative " + old + " vs. " + relativeAddress + ")");
	}

	public static byte[] assemble(ZAssemblerFile file, int externallyGivenVersion, String externallyGivenVersionSourceName)
	{
		int version;
		if(externallyGivenVersion <= 0)
			version = file.version().orElseThrow(() -> new IllegalArgumentException(
					"Z-version not given: neither by " + externallyGivenVersionSourceName + ", nor by .ZVERSION in file"));
		else if(file.version().isEmpty())
			version = externallyGivenVersion;
		else if(file.version().getAsInt() == externallyGivenVersion)
			version = externallyGivenVersion;
		else
			throw new IllegalArgumentException("Z-Version given by " + externallyGivenVersionSourceName + " mismatches .ZVERSION in file");

		ZAssembler assembler = new ZAssembler(version);
		assembler.add(file);
		return assembler.assemble();
	}
}
