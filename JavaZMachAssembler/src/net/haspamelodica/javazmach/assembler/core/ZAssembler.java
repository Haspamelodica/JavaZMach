package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.core.instructions.Opcode._unknown_instr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.Buffer;
import net.haspamelodica.javazmach.assembler.model.Dictionary;
import net.haspamelodica.javazmach.assembler.model.GlobalVarTable;
import net.haspamelodica.javazmach.assembler.model.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.LabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.MacroDeclaration;
import net.haspamelodica.javazmach.assembler.model.MacroEntry;
import net.haspamelodica.javazmach.assembler.model.MacroOrFileEntry;
import net.haspamelodica.javazmach.assembler.model.MacroReference;
import net.haspamelodica.javazmach.assembler.model.NamedValue;
import net.haspamelodica.javazmach.assembler.model.Routine;
import net.haspamelodica.javazmach.assembler.model.SectionDeclaration;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.assembler.model.ZObjectTable;
import net.haspamelodica.javazmach.core.instructions.Opcode;

public class ZAssembler
{
	private final int					version;
	private final Map<String, Opcode>	opcodesByNameLowercase;

	private final AssembledHeader				header;
	private final ConvergingEntriesAssembler	assembler;
	private final Map<String, MacroDeclaration>	macrosByName;

	private int nextMacroReferenceIdent;

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

		this.header = new AssembledHeader(version);
		this.assembler = new ConvergingEntriesAssembler(version);
		assembler.addEntry(header);

		this.macrosByName = new HashMap<>();
		// 0 is the main file
		this.nextMacroReferenceIdent = 1;
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
			case HeaderEntry headerEntry -> header.addEntry(headerEntry);
			case ZObjectTable table -> assembler.addEntry(new AssembledZObjectTable(table, version));
			case GlobalVarTable globals -> assembler.addEntry(new AssembledGlobals(globals));
			case Dictionary dictionary -> assembler.addEntry(new AssembledDictionary(dictionary, version));
			case SectionDeclaration section -> assembler.addEntry(new AssembledSectionDeclaration(section));
			case MacroDeclaration macroDeclaration -> macrosByName.put(macroDeclaration.name(), macroDeclaration);
			case MacroOrFileEntry e -> add(e, 0);
		}
	}

	public void add(MacroOrFileEntry entry, int macroReferenceIdent)
	{
		switch(entry)
		{
			case LabelDeclaration labelDeclaration -> assembler.addEntry(new AssembledLabelDeclaration(labelDeclaration.name()));
			case ZAssemblerInstruction instruction -> assembler.addEntry(new AssembledInstruction(instruction, version, opcodesByNameLowercase));
			case Routine routine -> assembler.addEntry(new AssembledRoutineHeader(routine, version));
			case Buffer buffer -> assembler.addEntry(new AssembledBuffer(buffer, version));
			case NamedValue namedValue -> assembler.addEntry(new AssembledNamedValue(namedValue));
			case MacroReference macroReference -> addMacroReference(macrosByName.get(macroReference.name()));
		}
	}

	private void addMacroReference(MacroDeclaration macro)
	{
		int macroReferenceIdent = nextMacroReferenceIdent ++;
		for(MacroEntry entry : macro.body())
			switch(entry)
			{
				case MacroOrFileEntry e -> add(e, macroReferenceIdent);
			}
	}

	public byte[] assemble()
	{
		return assembler.assembleUntilConvergence();
	}

	public static byte[] assemble(ZAssemblerFile file, OptionalInt externallyGivenVersion, String externallyGivenVersionSourceName)
	{
		int version;
		if(externallyGivenVersion.isEmpty())
			version = file.version().orElseGet(() -> defaultError(
					"Z-version not given: neither by " + externallyGivenVersionSourceName + ", nor by .ZVERSION in file"));
		else if(file.version().isEmpty())
			version = externallyGivenVersion.getAsInt();
		else if(file.version().getAsInt() == externallyGivenVersion.getAsInt())
			version = externallyGivenVersion.getAsInt();
		else
			return defaultError("Z-version given by " + externallyGivenVersionSourceName + " mismatches .ZVERSION in file");

		ZAssembler assembler = new ZAssembler(version);
		assembler.add(file);
		return assembler.assemble();
	}
}
