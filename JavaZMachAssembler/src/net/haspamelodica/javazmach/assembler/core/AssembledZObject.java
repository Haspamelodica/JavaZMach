package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.MacroContext.GLOBAL_MACRO_CONTEXT;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.ZAttribute;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledZObject
{
	private final Optional<String>		ident;
	private final AssembledZAttributes	attributes;
	private final int					index, parentIndex, siblingIndex, childIndex;
	private BigInteger					propAddress;

	public AssembledZObject(Optional<String> ident, List<ZAttribute> attributes, int index, int parentIndex, int siblingIndex, int childIndex, int version)
	{
		this.ident = ident;
		this.attributes = new AssembledZAttributes(attributes, version);
		this.index = index;
		this.parentIndex = parentIndex;
		this.siblingIndex = siblingIndex;
		this.childIndex = childIndex;
		this.propAddress = null;
	}

	public void updateResolvedValues(ValueReferenceResolver valueReferenceResolver)
	{
		propAddress = valueReferenceResolver.resolveAbsoluteOrNull(new PropertiesLocation(index));
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		// object table is always in global context
		ident.ifPresent(i -> locationEmitter.emitLocation(new LabelLocation(GLOBAL_MACRO_CONTEXT.refId(), i), BigInteger.valueOf(index)));
		attributes.append(locationEmitter, memSeq, diagnosticHandler);
		memSeq.writeNextByte(parentIndex);
		memSeq.writeNextByte(siblingIndex);
		memSeq.writeNextByte(childIndex);
		if(propAddress == null)
		{
			diagnosticHandler.error(String.format("Properties for object %d not found", index));
			memSeq.writeNextWord(0);
		} else
		{
			memSeq.writeNextWord(bigintIntChecked(16, propAddress,
					i -> String.format("Property location cannot be more than 2 bytes but is: %s", i.toString()), diagnosticHandler));
		}
	}
}
