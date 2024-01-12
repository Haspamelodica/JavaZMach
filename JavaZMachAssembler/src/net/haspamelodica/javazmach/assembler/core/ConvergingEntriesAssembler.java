package net.haspamelodica.javazmach.assembler.core;

import static java.math.BigInteger.ZERO;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultEmit;
import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.SectionLikeLocation.FILE_CHECKSUM;
import static net.haspamelodica.javazmach.assembler.core.SectionLikeLocation.FILE_END;
import static net.haspamelodica.javazmach.assembler.core.SectionLikeLocation.HIGH_MEM_BASE;
import static net.haspamelodica.javazmach.assembler.core.SectionLikeLocation.STATIC_MEM_BASE;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.assembler.model.Section.DYNAMIC;
import static net.haspamelodica.javazmach.assembler.model.Section.HIGH;
import static net.haspamelodica.javazmach.assembler.model.Section.STATIC;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.Section;
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
		Map<ValueReference, BigInteger> locations = new HashMap<>();
		List<Diagnostic> diagnostics;
		for(boolean isFirst = true;; isFirst = false)
		{
			diagnostics = new ArrayList<>();
			ValueReferenceManagerImpl locationManager = new ValueReferenceManagerImpl(locations, () -> BigInteger.valueOf(memSeq.getAddress()), isFirst);

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

	public void assembleOneIteration(DiagnosticHandler diagnosticHandler, ValueReferenceManagerImpl locationManager)
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
		emitSectionLocations(locationManager, diagnosticHandler);
	}

	private void emitSectionLocations(ValueReferenceManager locationManager, DiagnosticHandler diagnosticHandler)
	{
		record SectionTypeHint(BigInteger start, BigInteger end, Section type)
		{}
		record BigIntegerSummary(BigInteger min, BigInteger max)
		{}
		Map<Section, BigIntegerSummary> sectionTypeSummaries = entries
				.stream()
				.map(e -> new SectionTypeHint(locationManager.resolveAbsoluteOrNull(new EntryStartLocation(e)),
						locationManager.resolveAbsoluteOrNull(new EntryEndLocation(e)), switch(e)
						{
							case AssembledHeader entry -> DYNAMIC;
							case AssembledInstruction entry -> HIGH;
							case AssembledRoutineHeader entry -> HIGH;
							case AssembledZObjectTable entry -> DYNAMIC;
							case AssembledGlobals entry -> DYNAMIC;
							// labels or section declarations by themselves don't do anything
							case AssembledLabelDeclaration entry -> null;
							case AssembledSectionDeclaration entry -> null;
						}))
				.filter(h -> h.type() != null)
				.collect(Collectors.groupingBy(SectionTypeHint::type, Collectors.mapping(h -> new BigIntegerSummary(h.start(), h.end()),
						Collectors.collectingAndThen(
								Collectors.reducing((s1, s2) -> new BigIntegerSummary(s1.min().min(s2.min()), s2.max().max(s2.max()))),
								Optional::get))));

		BigIntegerSummary dynamicSummary = sectionTypeSummaries.get(DYNAMIC);
		BigIntegerSummary staticSummary = sectionTypeSummaries.get(STATIC);
		BigIntegerSummary highSummary = sectionTypeSummaries.get(HIGH);
		BigInteger dynamicExplicitStart = locationManager.tryResolveAbsoluteOrNull(new ExplicitSectionLocation(DYNAMIC));
		BigInteger staticExplicitStart = locationManager.tryResolveAbsoluteOrNull(new ExplicitSectionLocation(STATIC));
		BigInteger highExplicitStart = locationManager.tryResolveAbsoluteOrNull(new ExplicitSectionLocation(HIGH));

		if(dynamicExplicitStart != null && dynamicExplicitStart.signum() != 0)
			diagnosticHandler.warning("Explicit start of dynamic section wasn't at beginning of file - dynamic memory always starts at beginning of file");

		BigInteger dynamicEntriesEnd = dynamicSummary != null ? dynamicSummary.max() : ZERO;

		BigInteger staticStart;
		if(staticExplicitStart != null)
			staticStart = staticExplicitStart;
		else if(staticSummary != null)
			staticStart = staticSummary.min();
		else
			staticStart = dynamicEntriesEnd;

		// max with staticStart to ensure staticEnd >= staticStart even when staticStart is given explicitly
		BigInteger staticEntriesEnd = staticSummary != null ? staticSummary.max().max(staticStart) : staticStart;

		BigInteger highStart;
		if(highExplicitStart != null)
			highStart = highExplicitStart;
		else if(highSummary != null)
			highStart = highSummary.min();
		else
			highStart = staticEntriesEnd;

		if(dynamicEntriesEnd.compareTo(staticStart) > 0)
			diagnosticHandler.info("Dynamic entries in static memory");
		if(dynamicEntriesEnd.compareTo(highStart) > 0)
			diagnosticHandler.info("Dynamic entries in high memory");
		// Static entries in high memory are fine, so don't emit a warning for that: static mem is allowed to overlap with high mem.
		if(staticStart.compareTo(highStart) > 0)
			diagnosticHandler.warning("High memory overlaps with dynamic memory - this will likely fail");

		locationManager.emitValueReference(STATIC_MEM_BASE, staticStart);
		locationManager.emitValueReference(HIGH_MEM_BASE, highStart);
		locationManager.emitValueReferenceHere(FILE_END, addr -> BigInteger.valueOf(computeFileEnd(bigintIntChecked(32, addr, (b) -> "Address does not fit in 32bit integer. This must be an assembler bug", diagnosticHandler))));
		locationManager.emitValueReference(FILE_CHECKSUM, computeChecksum());
	}

	private BigInteger computeChecksum()
	{
		// effectively, we align fileEnd down to story file size
		int fileEnd = computeFileEnd(memSeq.getAddress());
		int checksum = 0;
		byte[] data = mem.data();
		for(int i = 0x40; i < fileEnd * storyfileSizeDivisor; i ++)
		{
			checksum += data[i] & 0xff;
		}
		return BigInteger.valueOf(checksum & 0xffff);
	}

	private int computeFileEnd(int maxAddress)
	{
		return maxAddress / storyfileSizeDivisor;
	}

	public void addEntry(AssembledEntry entry)
	{
		entries.add(entry);
	}
}
