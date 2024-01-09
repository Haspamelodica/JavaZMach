package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultEmit;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

// TODO separation of concerns between this and ZAssembler is a bit weird - maybe move entire managing of entries, mem, memSeq here?
public class ConvergingEntriesAssembler
{
	private final List<AssembledEntry>			entries;
	private final NoRangeCheckMemory			mem;
	private final SequentialMemoryWriteAccess	memSeq;
	private final int							codeStart;

	public ConvergingEntriesAssembler(List<AssembledEntry> entries, NoRangeCheckMemory codeMem, SequentialMemoryWriteAccess codeSeq, int codeStart)
	{
		this.entries = entries;
		this.mem = codeMem;
		this.memSeq = codeSeq;
		this.codeStart = codeStart;
	}

	public Map<Location, BigInteger> assembleUntilConvergence()
	{
		Map<Location, BigInteger> locations = new HashMap<>();
		List<Diagnostic> diagnostics;
		for(boolean isFirst = true;; isFirst = false)
		{
			diagnostics = new ArrayList<>();
			DiagnosticHandler diagnosticHandler = diagnostics::add;
			LocationManagerImpl locationManager = new LocationManagerImpl(locations,
					() -> BigInteger.valueOf(codeStart + memSeq.getAddress()), isFirst);

			// Yes, do this every iteration - they might have changed.
			// Also, the way in which locations are updated in LocationManagerImpl means that after two iterations,
			// the initial values won't be accessible anymore.
			putSectionLocations(locations);
			for(AssembledEntry entry : entries)
			{
				entry.updateResolvedValues(locationManager);
				entry.append(locationManager, memSeq, diagnosticHandler);
			}

			if(!locationManager.nextIterationNecessary())
				break;

			// If we get here, we haven't converged yet.
			// Implicitly discard all diagnostics; even errors might disappear later.
			// But don't forget to update locations, codeMem and codeSeq.
			locations = locationManager.getNewlySetLocations();
			memSeq.setAddress(0);
			mem.clear();
		}

		// Convergence reached! Now handle all diagnostics.
		//TODO stack trace is lost this way - maybe record in list of diagnostics if this is worth the effort?
		for(Diagnostic diagnostic : diagnostics)
			defaultEmit(diagnostic);

		return locations;
	}

	private void putSectionLocations(Map<Location, BigInteger> locations)
	{
		// Done this way instead of just a sequence of put's to let this cause compiler errors
		// if a Section has been forgotten or is added later
		for(Section section : Section.values())
			locations.put(section, switch(section)
			{
				case HIGH_MEM_BASE -> BigInteger.valueOf(codeStart);
				// Unfortunately we can't throw an UnsupportedOperationException here.
				case STATIC_MEM_BASE -> null;
			});
	}
}
