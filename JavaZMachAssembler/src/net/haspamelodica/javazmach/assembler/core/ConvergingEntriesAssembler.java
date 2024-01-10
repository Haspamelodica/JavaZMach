package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultEmit;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.Section.FILE_END;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

// TODO separation of concerns between this and ZAssembler is a bit weird - maybe move entire managing of entries, mem, memSeq here?
public class ConvergingEntriesAssembler
{
	private final List<AssembledEntry>			entries;
	private final NoRangeCheckMemory			mem;
	private final SequentialMemoryWriteAccess	memSeq;
	private final int							storyfileSizeDivisor;

	public ConvergingEntriesAssembler(int version)
	{
		this.entries = new ArrayList<>();
		this.mem = new NoRangeCheckMemory();
		this.memSeq = new SequentialMemoryWriteAccess(mem);
		this.storyfileSizeDivisor = switch(version)
		{
			case 1, 2, 3 -> 2;
			case 4, 5 -> 4;
			case 6, 7, 8 -> 8;
			default -> defaultError("Unknown version: " + version + "; don't know how file length is stored");
		};
	}

	public byte[] assembleUntilConvergence()
	{
		Map<Location, BigInteger> locations = new HashMap<>();
		List<Diagnostic> diagnostics;
		for(boolean isFirst = true;; isFirst = false)
		{
			diagnostics = new ArrayList<>();
			LocationManagerImpl locationManager = new LocationManagerImpl(locations, () -> BigInteger.valueOf(memSeq.getAddress()), isFirst);

			assembleOneIteration(diagnostics::add, locationManager);

			if(!locationManager.nextIterationNecessary())
				break;

			// If we get here, we haven't converged yet.
			// Implicitly discard all diagnostics; even errors might disappear later.
			// But don't forget to update locations, memSeq and mem.
			locations = locationManager.getNewlySetLocationsModifiable();
			memSeq.setAddress(0);
			mem.clear();
		}

		// Convergence reached! Now handle diagnostics.
		//TODO stack trace is lost this way - maybe record in list of diagnostics if this is worth the effort?
		for(Diagnostic diagnostic : diagnostics)
			defaultEmit(diagnostic);

		return mem.data();
	}

	public void assembleOneIteration(DiagnosticHandler diagnosticHandler, LocationManagerImpl locationManager)
	{
		for(AssembledEntry entry : entries)
		{
			// these are needed for the auto fields HighMemoryBase and StaticMemBase
			locationManager.emitLocationHere(new EntryStartLocation(entry));
			entry.updateResolvedValues(locationManager);
			entry.append(locationManager, memSeq, diagnosticHandler);
			locationManager.emitLocationHere(new EntryEndLocation(entry));
		}

		memSeq.alignToBytes(storyfileSizeDivisor, 0);
		emitSectionLocations(locationManager);
	}

	private void emitSectionLocations(LocationManager locationManager)
	{
		enum SectionType
		{
			DYNAMIC,
			STATIC,
			HIGH;
		}
		record SectionTypeHint(BigInteger start, BigInteger end, SectionType type)
		{}
		record BigIntegerSummary(BigInteger min, BigInteger max)
		{}
		Map<SectionType, BigIntegerSummary> sectionTypeSummaries = entries
				.stream()
				.map(e -> new SectionTypeHint(locationManager.resolveAbsoluteOrNull(new EntryStartLocation(e)),
						locationManager.resolveAbsoluteOrNull(new EntryEndLocation(e)), switch(e)
						{
							case AssembledHeader entry -> SectionType.DYNAMIC;
							case AssembledInstruction entry -> SectionType.HIGH;
							case AssembledRoutineHeader entry -> SectionType.HIGH;
							// a label by itself doesn't do anything
							case LabelEntry entry -> null;
							case AssembledZObjectTable entry -> SectionType.DYNAMIC;
							case AssembledGlobals entry -> SectionType.DYNAMIC;
						}))
				.filter(h -> h.type() != null)
				.collect(Collectors.groupingBy(SectionTypeHint::type, Collectors.mapping(h -> new BigIntegerSummary(h.start(), h.end()),
						Collectors.collectingAndThen(
								Collectors.reducing((s1, s2) -> new BigIntegerSummary(s1.min().min(s2.min()), s2.max().max(s2.max()))),
								Optional::get))));

		BigIntegerSummary dynamicSummary = sectionTypeSummaries.get(SectionType.DYNAMIC);
		BigIntegerSummary staticSummary = sectionTypeSummaries.get(SectionType.STATIC);
		BigIntegerSummary highSummary = sectionTypeSummaries.get(SectionType.HIGH);
		//TODO do something with this summary

		locationManager.emitLocationHere(FILE_END, addr -> addr.divide(BigInteger.valueOf(storyfileSizeDivisor)));
	}

	public void addEntry(AssembledEntry entry)
	{
		entries.add(entry);
	}
}
