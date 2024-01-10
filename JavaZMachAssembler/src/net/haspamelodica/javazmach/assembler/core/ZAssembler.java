package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultInfo;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.materializeByteSequence;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.versionRangeString;
import static net.haspamelodica.javazmach.core.header.HeaderField.AbbrevTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.AlphabetTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.DictionaryLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.FileLength;
import static net.haspamelodica.javazmach.core.header.HeaderField.GlobalVarTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.HighMemoryBase;
import static net.haspamelodica.javazmach.core.header.HeaderField.ObjTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.StaticMemBase;
import static net.haspamelodica.javazmach.core.header.HeaderField.Version;
import static net.haspamelodica.javazmach.core.instructions.Opcode._unknown_instr;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.LabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.Routine;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.assembler.model.ZObjectTable;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;
import net.haspamelodica.javazmach.core.memory.WritableMemory;

public class ZAssembler
{
	private static final Set<HeaderField> AUTO_FIELDS = Set.of(FileLength, Version, AlphabetTableLoc, HighMemoryBase, ObjTableLoc);

	private final int					version;
	private final Map<String, Opcode>	opcodesByNameLowercase;

	//TODO make this an AssembledEntry
	private final NoRangeCheckMemory					header;
	private final List<AssembledIntegralHeaderField>	assembledHeaderFields;

	private final Set<HeaderField>	setFields;
	private final Set<HeaderField>	partiallySetBitfields;

	private final List<AssembledEntry>			assembledEntries;
	private final NoRangeCheckMemory			mem;
	private final SequentialMemoryWriteAccess	memSeq;

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
		this.assembledEntries = new ArrayList<>();
		this.mem = new NoRangeCheckMemory();
		this.memSeq = new SequentialMemoryWriteAccess(mem);
		this.setFields = new HashSet<>();
		this.partiallySetBitfields = new HashSet<>();
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
			case Routine routine -> add(routine);
			case ZObjectTable table -> add(table);
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
				byte[] value = materializeByteSequence(byteSequence, (error) -> "Error in field " + field + ": " + error);

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
		assembledEntries.add(new LabelEntry(labelDeclaration.name()));
	}

	public void add(ZAssemblerInstruction instruction)
	{
		assembledEntries.add(new AssembledInstruction(instruction, version, opcodesByNameLowercase));
	}

	public void add(Routine routine)
	{
		assembledEntries.add(new AssembledRoutineHeader(routine, version));
	}

	public void add(ZObjectTable table)
	{
		assembledEntries.add(new AssembledZObjectTable(table, version));
	}

	public byte[] assemble()
	{
		preAssembleHeader();

		int headerStart = 0;
		int headerEnd = headerStart + header.currentSize();
		int codeStart = headerEnd;

		// Try resolving references until sizes and code locations stop changing.
		ConvergingEntriesAssembler codeAssembler = new ConvergingEntriesAssembler(assembledEntries, mem, memSeq, codeStart);
		Map<Location, BigInteger> locations = codeAssembler.assembleUntilConvergence();

		// Assembling converged; code size is known!
		int codeEnd = codeStart + mem.currentSize();
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
		assembleHeader(locations::get, storyfileSize / storyfileSizeDivisor);

		byte[] result = new byte[storyfileSize];
		System.arraycopy(header.data(), 0, result, headerStart, header.currentSize());
		System.arraycopy(mem.data(), 0, result, codeStart, mem.currentSize());
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

		enum SectionType
		{
			DYNAMIC,
			STATIC,
			HIGH;
		}
		record SectionTypeHint(Location location, SectionType type)
		{}
		assembledEntries
				.stream()
				.map(e -> new SectionTypeHint(new EntryStartLocation(e), switch(e)
				{
					case AssembledInstruction entry -> SectionType.HIGH;
					case AssembledRoutineHeader entry -> SectionType.HIGH;
					case LabelEntry entry -> null;
					case AssembledZObjectTable entry -> SectionType.DYNAMIC;
				}))
				.filter(Objects::nonNull);
		//TODO do something with this stream.

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
					case HighMemoryBase -> storeLocationInField(header, HighMemoryBase, Section.HIGH_MEM_BASE, locationResolver);
					case DictionaryLoc -> storeLocationInField(header, DictionaryLoc, SpecialDataStructureLocation.DICTIONARY, locationResolver);
					case ObjTableLoc -> storeLocationInField(header, ObjTableLoc, SpecialDataStructureLocation.OBJ_TABLE, locationResolver);
					case GlobalVarTableLoc -> storeLocationInField(header, GlobalVarTableLoc, SpecialDataStructureLocation.GLOBAL_VAR_TABLE, locationResolver);
					case StaticMemBase -> storeLocationInField(header, StaticMemBase, Section.STATIC_MEM_BASE, locationResolver);
					case AbbrevTableLoc -> storeLocationInField(header, AbbrevTableLoc, SpecialDataStructureLocation.ABBREV_TABLE, locationResolver);
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

	private void storeLocationInField(WritableMemory header, HeaderField field, Location location, LocationResolver resolver)
	{
		BigInteger resolvedValue = resolver.resolveAbsoluteOrNull(location);
		if(resolvedValue == null)
		{
			defaultError("Location " + location + " not defined!");
		}
		int val = bigintIntChecked(field.len * 8, resolvedValue, bigint -> "section address too large for field of " + field.len + "bytes: "
				+ bigint);
		HeaderParser.setFieldUnchecked(header, field, val);
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
