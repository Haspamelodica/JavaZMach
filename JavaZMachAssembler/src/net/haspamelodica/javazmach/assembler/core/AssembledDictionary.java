package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.util.Iterator;
import java.util.List;

import net.haspamelodica.javazmach.assembler.model.Dictionary;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public final class AssembledDictionary implements AssembledEntry
{
	private static final int						MAX_DICTIONARY_ENTRIES	= 0xffff;
	private final List<AssembledDictionaryEntry>	entries;
	private final int								maxEntrySize;

	public AssembledDictionary(Dictionary dictionary, int version)
	{
		entries = dictionary.entries().stream().map(e -> new AssembledDictionaryEntry(e.key(), e.elements(), version)).sorted((e1, e2) ->
		{
			Iterator<Byte> it1 = e1.getKeyBytes().iterator();
			Iterator<Byte> it2 = e2.getKeyBytes().iterator();
			while(true)
			{
				if(!it1.hasNext() || !it2.hasNext())
				{
					if(!it1.hasNext() && !it2.hasNext())
					{
						return 0;
					} else if(!it1.hasNext())
					{
						return -1;
					} else
					{
						return 1;
					}
				}

				int cmp = it1.next().compareTo(it2.next());
				if(cmp == 0)
					continue;
				return cmp;
			}
		}).toList();

		List<Byte> bytes = null;
		for(AssembledDictionaryEntry entry : entries)
		{
			if(entry.getKeyBytes().equals(bytes))
			{
				defaultError("Duplicate key in dictionary");
			} else
			{
				bytes = entry.getKeyBytes();
			}
		}

		maxEntrySize = entries.stream().mapToInt(e -> e.getTotalSize()).max().orElse(0);
		if(entries.size() >= MAX_DICTIONARY_ENTRIES)
		{
			defaultError("Too many dictionary entries. Maximum allowed: %d Actual: %d".formatted(MAX_DICTIONARY_ENTRIES, entries.size()));
		}
	}

	@Override
	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		entries.forEach(e -> e.updateResolvedValues(valueReferenceResolver));
	}

	@Override
	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		locationEmitter.emitLocationHere(SpecialDataStructureLocation.DICTIONARY);
		// TODO: separators
		memSeq.writeNextByte(0);
		memSeq.writeNextByte(maxEntrySize);
		memSeq.writeNextWord(entries.size());
		entries.forEach(e -> e.appendPadded(locationEmitter, memSeq, diagnosticHandler, maxEntrySize));
	}

}
