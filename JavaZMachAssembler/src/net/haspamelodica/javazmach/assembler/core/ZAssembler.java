package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultInfo;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.versionRangeString;
import static net.haspamelodica.javazmach.core.header.HeaderField.AlphabetTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.FileLength;
import static net.haspamelodica.javazmach.core.header.HeaderField.Version;
import static net.haspamelodica.javazmach.core.instructions.Opcode._unknown_instr;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.core.CodeLocation.InstructionPart;
import net.haspamelodica.javazmach.assembler.model.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.ByteSequenceElement;
import net.haspamelodica.javazmach.assembler.model.CharLiteral;
import net.haspamelodica.javazmach.assembler.model.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.LabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.NumberLiteral;
import net.haspamelodica.javazmach.assembler.model.Routine;
import net.haspamelodica.javazmach.assembler.model.StringLiteral;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class ZAssembler
{
	private static final Set<HeaderField> AUTO_FIELDS = Set.of(FileLength, Version, AlphabetTableLoc);

	private final int					version;
	private final Map<String, Opcode>	opcodesByNameLowercase;

	private final NoRangeCheckMemory					header;
	private final List<AssembledIntegralHeaderField>	assembledHeaderFields;

	private final Set<HeaderField>	setFields;
	private final Set<HeaderField>	partiallySetBitfields;

	private final List<AssembledInstruction>	code;
	private final NoRangeCheckMemory			codeMem;
	private final SequentialMemoryWriteAccess	codeSeq;
	private final Map<String, Location>			labelLocations;

	public ZAssembler(int version)
	{
		this.version = version;
		this.opcodesByNameLowercase = Arrays
				.stream(Opcode.values())
				.filter(o -> o != _unknown_instr)
				.filter(o -> version >= o.minVersion)
				.filter(o -> version <= o.maxVersion || o.maxVersion <= 0)
				// careful: don't use method "name()", but member "name".
				.collect(Collectors.toUnmodifiableMap(o -> o.name.toLowerCase(), o -> o));

		this.header = new NoRangeCheckMemory();
		this.assembledHeaderFields = new ArrayList<>();
		this.code = new ArrayList<>();
		this.codeMem = new NoRangeCheckMemory();
		this.codeSeq = new SequentialMemoryWriteAccess(codeMem);
		this.setFields = new HashSet<>();
		this.partiallySetBitfields = new HashSet<>();
		this.labelLocations = new HashMap<>();
	}

	public void add(ZAssemblerFile file)
	{
		if(file.version().isPresent() && file.version().getAsInt() != version)
			defaultError("Version mismatch");

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
			case Routine routine -> System.err.println("Uh-oh");
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
			defaultError("Unknown header field: " + headerEntry.name());
			return;
		}

		boolean isBitfieldEntry = field.bitfield != null;
		if(isBitfieldEntry)
			partiallySetBitfields.add(field.bitfield);

		if(version < field.minVersion || (field.maxVersion > 0 && version > field.maxVersion))
			defaultError("Field " + field + " does not exist in version " + version
					+ "; only " + versionRangeString(field.minVersion, field.maxVersion));
		if(!setFields.add(field))
			defaultWarning("Field " + field + " set twice - old value will be overwritten");
		if(field.isBitfield && partiallySetBitfields.contains(field))
			defaultWarning("Bitfield " + field
					+ " is set after some bitfield entries have been set - old bitfield entry values will be overwritten");
		if(field.isRst)
			defaultInfo("Field " + field + " is Rst - will usually be overwritten by interpreter on startup");

		if(AUTO_FIELDS.contains(field))
			defaultInfo("Automatically computed value of " + field + " is overwritten with explicit value");

		switch(headerEntry.value())
		{
			case IntegralValue value -> assembledHeaderFields.add(isBitfieldEntry
					? new AssembledIntegralBitfieldHeaderField(field, value)
					: new AssembledIntegralRegularHeaderField(field, value));
			case ByteSequence byteSequence ->
			{
				int length = byteSequence
						.elements()
						.stream()
						.mapToInt(e -> switch(e)
						{
							case NumberLiteral elementInteger -> 1;
							case StringLiteral elementString -> elementString.value().length();
							case CharLiteral elementChar -> 1;
						})
						.sum();
				byte[] value = new byte[length];
				int i = 0;
				for(ByteSequenceElement elementUncasted : byteSequence.elements())
					switch(elementUncasted)
					{
						case NumberLiteral element -> value[i ++] = (byte) bigintIntChecked(8,
								element.value(), bigint -> "byte literal out of range: " + bigint + " for field " + field);
						case StringLiteral element ->
						{
							System.arraycopy(element.value().getBytes(StandardCharsets.US_ASCII), 0, value, 0, element.value().length());
							i += element.value().length();
						}
						case CharLiteral element ->
						{
							if((element.value() & ~0x7f) != 0)
								defaultError("char literal out of range (not ASCII): " + element.value()
										+ " for field " + field);
							value[i ++] = (byte) element.value();
						}
					};

				if(isBitfieldEntry)
					defaultError("Setting a bitfield entry to a byte sequence "
							+ "(not a single integer literal) is nonsensical: " + field);

				if(field.len > value.length)
					defaultWarning("Byte sequence value for field " + field + " is too short ("
							+ value.length + "<" + field.len + "); will be padded with nullbytes");
				else if(field.len < value.length)
					defaultError("Byte sequence value for field " + field + " is too long: "
							+ value.length + ">" + field.len);

				HeaderParser.setFieldUncheckedBytes(header, field, value);
			}
		}
	}

	public void add(LabelDeclaration labelDeclaration)
	{
		defineLabelCodeHere(labelDeclaration.name());
	}

	public void add(ZAssemblerInstruction instruction)
	{
		code.add(new AssembledInstruction(instruction, version, opcodesByNameLowercase));
	}

	public byte[] assemble()
	{
		preAssembleHeader();

		int headerStart = 0;
		int headerEnd = headerStart + header.currentSize();
		int codeStart = headerEnd;

		// Try resolving references until sizes and code locations stop changing.
		CodeAssembler codeAssembler = new CodeAssembler(code, codeMem, codeSeq, labelLocations, codeStart);
		codeAssembler.assembleUntilConvergence();

		// Assembling converged; code size is known!
		int codeEnd = codeStart + codeMem.currentSize();
		// Compute entire storyfile size; maybe pad
		int storyfileSizeDivisor = switch(version)
		{
			case 1, 2, 3 -> 2;
			case 4, 5 -> 4;
			case 6, 7, 8 -> 8;
			default -> defaultError("Unknown version: " + version + "; don't know how file length is stored");
		};
		int storyfileSize = codeEnd;
		// Conecptually VERY inefficient, but in practice probably not too bad, considering how small the possible divisors are.
		while(storyfileSize % storyfileSizeDivisor != 0)
			storyfileSize ++;

		// From here on, sizes and code locations are known and frozen.

		// Now that all sizes and locations are known, we can assemble the header.
		assembleHeader(codeAssembler, storyfileSize / storyfileSizeDivisor);

		byte[] result = new byte[storyfileSize];
		System.arraycopy(header.data(), 0, result, headerStart, header.currentSize());
		System.arraycopy(codeMem.data(), 0, result, codeStart, codeMem.currentSize());
		// No need to care for padding: If storyfile is padded, the padding bytes will already be 0.
		return result;
	}

	private void preAssembleHeader()
	{
		// Write bogus values to all header fields to ensure header is grown to correct size
		for(AssembledIntegralHeaderField assembledIntegralHeaderField : assembledHeaderFields)
			HeaderParser.setFieldUnchecked(header, assembledIntegralHeaderField.getField(), 0);

		// Ensure header is at least 0x40 long by padding with nullbytes
		if(header.currentSize() < 0x40)
			// We are sure that this doesn't overwrite anything because we just checked that the header is shorter.
			// Also, the header values themselves will only be inserted later on.
			header.writeByte(0x3f, 0x00);
	}

	private void assembleHeader(LocationResolver locationResolver, int predividedStoryfileSize)
	{
		for(AssembledIntegralHeaderField assembledField : assembledHeaderFields)
			assembledField.assemble(header, locationResolver);

		for(HeaderField automaticField : AUTO_FIELDS)
			if(!setFields.contains(automaticField))
				switch(automaticField)
				{
					case FileLength -> HeaderParser.setFieldUnchecked(header, FileLength, bigintIntChecked(FileLength.len * 8,
							BigInteger.valueOf(predividedStoryfileSize),
							v -> "Storyfile too large: storyfile size header field would have to be " + v));
					case Version -> HeaderParser.setFieldUnchecked(header, Version, version);
					// we don't support custom alphabets (yet), so set this to 0
					case AlphabetTableLoc -> HeaderParser.setFieldUnchecked(header, AlphabetTableLoc, 0);
					default -> defaultError("Field " + automaticField
							+ " is supposedly auto, but is not handled by the assembler!? This is an assembler bug.");
				}

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
			defaultInfo("The following non-Rst header fields have no explicit value and will default to 0: " + unsetHeaderFieldsStr);
	}

	private void defineLabelCodeHere(String label)
	{
		defineLabel(label, codeLocationHere());
	}

	private void defineLabel(String label, Location location)
	{
		Location old = labelLocations.put(label, location);
		if(old != null)
			defaultError("Duplicate label: " + label);
	}

	private Location codeLocationHere()
	{
		if(code.isEmpty())
			return SimpleLocation.CODE_START;
		else
			return new CodeLocation(code.get(code.size() - 1), InstructionPart.AFTER);
	}

	public static byte[] assemble(ZAssemblerFile file, int externallyGivenVersion, String externallyGivenVersionSourceName)
	{
		int version;
		if(externallyGivenVersion <= 0)
			version = file.version().orElseThrow(() -> defaultError(
					"Z-version not given: neither by " + externallyGivenVersionSourceName + ", nor by .ZVERSION in file"));
		else if(file.version().isEmpty())
			version = externallyGivenVersion;
		else if(file.version().getAsInt() == externallyGivenVersion)
			version = externallyGivenVersion;
		else
			return defaultError("Z-version given by " + externallyGivenVersionSourceName + " mismatches .ZVERSION in file");

		ZAssembler assembler = new ZAssembler(version);
		assembler.add(file);
		return assembler.assemble();
	}
}
