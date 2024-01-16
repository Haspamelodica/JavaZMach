package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intConst;
import static net.haspamelodica.javazmach.assembler.core.AssemblerIntegralValue.intLoc;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultInfo;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultWarning;
import static net.haspamelodica.javazmach.assembler.core.SectionLikeLocation.FILE_CHECKSUM;
import static net.haspamelodica.javazmach.assembler.core.SectionLikeLocation.FILE_END;
import static net.haspamelodica.javazmach.assembler.core.SectionLikeLocation.HIGH_MEM_BASE;
import static net.haspamelodica.javazmach.assembler.core.SectionLikeLocation.STATIC_MEM_BASE;
import static net.haspamelodica.javazmach.assembler.core.SpecialDataStructureLocation.DICTIONARY;
import static net.haspamelodica.javazmach.assembler.core.SpecialDataStructureLocation.GLOBAL_VAR_TABLE;
import static net.haspamelodica.javazmach.assembler.core.SpecialDataStructureLocation.OBJ_TABLE;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.materializeByteSequence;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.versionRangeString;
import static net.haspamelodica.javazmach.core.header.HeaderField.AlphabetTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.DictionaryLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.FileChecksum;
import static net.haspamelodica.javazmach.core.header.HeaderField.FileLength;
import static net.haspamelodica.javazmach.core.header.HeaderField.GlobalVarTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.HighMemoryBase;
import static net.haspamelodica.javazmach.core.header.HeaderField.ObjTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.StaticMemBase;
import static net.haspamelodica.javazmach.core.header.HeaderField.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledHeader implements AssembledEntry
{
	private final int									version;
	private final NoRangeCheckMemory					header;
	private final List<AssembledIntegralHeaderField>	assembledHeaderFields;

	private final Set<HeaderField>	setFields;
	private final Set<HeaderField>	partiallySetBitfields;

	private final Map<HeaderField, AssembledIntegralHeaderField> activeAutoFields;

	public AssembledHeader(int version)
	{
		this.version = version;
		this.header = new NoRangeCheckMemory();
		this.assembledHeaderFields = new ArrayList<>();

		this.setFields = new HashSet<>();
		this.partiallySetBitfields = new HashSet<>();

		Map<HeaderField, AssemblerIntegralValue> autoFields = new HashMap<>();
		autoFields.put(FileLength, intLoc(FILE_END));
		autoFields.put(FileChecksum, intLoc(FILE_CHECKSUM));
		autoFields.put(Version, intConst(version));
		// we don't support custom alphabets (yet), so set this to 0
		autoFields.put(AlphabetTableLoc, intConst(0));
		autoFields.put(HighMemoryBase, intLoc(HIGH_MEM_BASE));
		autoFields.put(DictionaryLoc, intLoc(DICTIONARY));
		autoFields.put(ObjTableLoc, intLoc(OBJ_TABLE));
		autoFields.put(GlobalVarTableLoc, intLoc(GLOBAL_VAR_TABLE));
		autoFields.put(StaticMemBase, intLoc(STATIC_MEM_BASE));
		//autoFields.put(AbbrevTableLoc, intLoc(ABBREV_TABLE));

		this.activeAutoFields = autoFields.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
				e -> new AssembledIntegralRegularHeaderField(e.getKey(), e.getValue()),
				(a, b) -> defaultError("Map had same key twice"), HashMap::new));

		// Ensure header is at least 0x40 long by padding with nullbytes - note that NoRangeCheckMemory#writeByte automatically pads.
		// We are sure that this doesn't overwrite anything because the header values themselves will only be inserted later on.
		header.writeByte(0x3f, 0x00);
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		assembledHeaderFields.forEach(f -> f.updateResolvedValues(valueReferenceResolver));
		activeAutoFields.values().forEach(f -> f.updateResolvedValues(valueReferenceResolver));
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		assembledHeaderFields.forEach(f -> f.assemble(header, diagnosticHandler));
		activeAutoFields.values().forEach(f -> f.assemble(header, diagnosticHandler));
		warnUnsetHeaderfields(diagnosticHandler);

		memSeq.writeNextBytes(header.data());
	}

	private void warnUnsetHeaderfields(DiagnosticHandler diagnosticHandler)
	{
		List<HeaderField> unsetHeaderFields = Arrays.stream(HeaderField.values())
				.filter(f -> version >= f.minVersion)
				.filter(f -> f.maxVersion <= 0 || version <= f.maxVersion)
				.filter(f -> !f.isRst)
				.filter(f -> !activeAutoFields.containsKey(f))
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
			diagnosticHandler.info("The following non-Rst header fields have no explicit value and will default to 0: " + unsetHeaderFieldsStr);
	}

	public void addEntry(HeaderEntry headerEntry)
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

		if(activeAutoFields.remove(field) != null)
			defaultInfo("Automatically computed value of " + field + " is overwritten with explicit value");

		switch(headerEntry.value())
		{
			case IntegralValue value -> assembledHeaderFields.add(isBitfieldEntry
					? new AssembledIntegralBitfieldHeaderField(field, value)
					: new AssembledIntegralRegularHeaderField(field, value));
			case ByteSequence byteSequence ->
			{
				byte[] value = materializeByteSequence(byteSequence, version, (error) -> "Error in field " + field + ": " + error);

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
}
