package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.GlobalVarTable;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledGlobals implements AssembledEntry
{
	private final static int GLOBALS_COUNT = 240;
	private final List<AssembledGlobal> globals;

	public AssembledGlobals(GlobalVarTable globals)
	{
		if(globals.globals().size() > GLOBALS_COUNT) {
			defaultError("Too many globals specified. Maximum is %d, got %d".formatted(GLOBALS_COUNT, globals.globals().size()));
		}
		this.globals = globals.globals().stream().map(AssembledGlobal::new).toList();
	}

	@Override
	public void updateResolvedValues(LocationResolver locationResolver)
	{
		this.globals.forEach(g -> g.updateResolvedValues(locationResolver));
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocationHere(SpecialDataStructureLocation.GLOBAL_VAR_TABLE);
		this.globals.forEach(g -> g.append(locationEmitter, memSeq, diagnosticHandler));
		for (int i = this.globals.size(); i < GLOBALS_COUNT; i++) {
			// tbd: the spec says that the globals table is 240 words in
			// size, but does it make sense to actually allocate all this space
			// if only some globals are named? The other ones should be inaccessible,
			// once indexed access (e.g. 'g12') is completely phased out.
			// We could let the table overlap with the following data structure in
			// entries that are never accessed.
			memSeq.writeNextWord(0);
		}
	}

}
