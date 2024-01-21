package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.MacroContext.FIRST_NONGLOBAL_MACRO_REFID;
import static net.haspamelodica.javazmach.assembler.core.MacroContext.GLOBAL_MACRO_CONTEXT;
import static net.haspamelodica.javazmach.core.instructions.Opcode._unknown_instr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.core.MacroContext.ResolvedMacroArgumentWithContext;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledBuffer;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledDictionary;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledGlobals;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledHeader;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledInstruction;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledLabelDeclaration;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledNamedValue;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledRoutineHeader;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledSectionDeclaration;
import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledZObjectTable;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.entries.Buffer;
import net.haspamelodica.javazmach.assembler.model.entries.Dictionary;
import net.haspamelodica.javazmach.assembler.model.entries.GlobalVarTable;
import net.haspamelodica.javazmach.assembler.model.entries.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.entries.LabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.MacroDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.MacroEntry;
import net.haspamelodica.javazmach.assembler.model.entries.MacroOrFileEntry;
import net.haspamelodica.javazmach.assembler.model.entries.MacroParamLabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.MacroReference;
import net.haspamelodica.javazmach.assembler.model.entries.NamedValue;
import net.haspamelodica.javazmach.assembler.model.entries.Routine;
import net.haspamelodica.javazmach.assembler.model.entries.SectionDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.entries.ZAssemblerInstruction;
import net.haspamelodica.javazmach.assembler.model.entries.ZObjectTable;
import net.haspamelodica.javazmach.core.instructions.Opcode;

public class ZAssembler
{
	private final int					version;
	private final Map<String, Opcode>	opcodesByNameLowercase;

	private final AssembledHeader				header;
	private final ConvergingEntriesAssembler	assembler;
	private final Map<String, MacroDeclaration>	macrosByName;

	private int nextMacroRefId;

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
		this.nextMacroRefId = FIRST_NONGLOBAL_MACRO_REFID;
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
			case MacroOrFileEntry e -> add(e, GLOBAL_MACRO_CONTEXT);
		}
	}

	public void add(MacroOrFileEntry entry, MacroContext macroContext)
	{
		switch(entry)
		{
			case LabelDeclaration labelDeclaration -> assembler.addEntry(new AssembledLabelDeclaration(macroContext, labelDeclaration.name()));
			case ZAssemblerInstruction instruction -> assembler.addEntry(new AssembledInstruction(macroContext, instruction, version, opcodesByNameLowercase));
			case Routine routine -> assembler.addEntry(new AssembledRoutineHeader(macroContext, routine, version));
			case Buffer buffer -> assembler.addEntry(new AssembledBuffer(macroContext, buffer, version));
			case NamedValue namedValue -> assembler.addEntry(new AssembledNamedValue(macroContext, namedValue));
			case MacroReference macroReference -> addMacroReference(macroReference, macroContext);
		}
	}

	private void addMacroReference(MacroReference macroReference, MacroContext outerMacroContext)
	{
		MacroDeclaration macroDeclaration = macrosByName.get(macroReference.name());
		if(macroDeclaration == null)
			defaultError("No macro with name " + macroReference.name());

		int paramCount = macroDeclaration.params().size();
		if(paramCount != macroReference.args().size())
			defaultError("Macro " + macroReference.name() + " called with incorrect number of arguments: "
					+ "expected " + paramCount + " but was " + macroReference.args().size());

		Map<String, ResolvedMacroArgumentWithContext> macroArgs = new HashMap<>();
		for(int i = 0; i < paramCount; i ++)
			macroArgs.put(macroDeclaration.params().get(i).name(), outerMacroContext.resolveMacroArgument(macroReference.args().get(i)));

		MacroContext macroContext = new MacroContext(nextMacroRefId ++, macroArgs, outerMacroContext);

		for(MacroEntry entry : macroDeclaration.body())
			switch(entry)
			{
				case MacroOrFileEntry e -> add(e, macroContext);
				case MacroParamLabelDeclaration e -> assembler.addEntry(macroContext.resolveAssembledLabelDeclaration(e));
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
