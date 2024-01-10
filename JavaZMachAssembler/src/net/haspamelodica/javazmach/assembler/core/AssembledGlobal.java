package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;

import java.math.BigInteger;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.Global;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledGlobal
{
	private final Optional<IntegralValue>	defaultValue;
	private BigInteger						defaultValueResolved;

	public AssembledGlobal(Global global)
	{
		this.defaultValue = global.initialValue();
		this.defaultValueResolved = null;
	}

	public void updateResolvedValues(LocationResolver locationResolver)
	{
		defaultValueResolved = defaultValue.map(v -> ZAssemblerUtils.integralValueOrNull(v, locationResolver)).orElseGet(() -> BigInteger.valueOf(0));
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		if(defaultValueResolved == null)
		{
			diagnosticHandler.error("Failed to resolve initial value for global");
			memSeq.writeNextWord(0);
		} else
		{
			memSeq.writeNextWord(bigintIntChecked(16, defaultValueResolved, (b) -> "Default value %s too large for global. Must be at most 2 bytes".formatted(b)));
		}
	}
}
