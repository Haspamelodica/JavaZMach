package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.assembler.model.ZObjectTable;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledZObjectTable implements AssembledEntry
{
	AssembledDefaultProperties defaultProperties;

	public AssembledZObjectTable(ZObjectTable table, int version)
	{
		// TODO: linearize table
		defaultProperties = new AssembledDefaultProperties(table.defaultProperties(), version);
	}

	@Override
	public void updateResolvedValues(LocationResolver locationsAndLabels)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess codeSeq, DiagnosticHandler diagnosticHandler)
	{
		// TODO: find out how Sections are supposed to be emitted/whether they should be special locations
		//locationEmitter.emitLocationHere(Section.OBJ_TABLE);
		this.defaultProperties.append(locationEmitter, codeSeq, diagnosticHandler);
	}

}
