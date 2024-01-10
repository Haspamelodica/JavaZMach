package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;

import java.math.BigInteger;
import java.util.List;

import net.haspamelodica.javazmach.assembler.model.ZAttribute;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledZObject
{
	private AssembledZAttributes	attributes;
	private int						index, parentIndex, siblingIndex, childIndex;
	private BigInteger				propAddress;

	public AssembledZObject(List<ZAttribute> attributes, int index, int parentIndex, int siblingIndex, int childIndex, int version)
	{
		this.attributes = new AssembledZAttributes(attributes, version);
		this.index = index;
		this.parentIndex = parentIndex;
		this.siblingIndex = siblingIndex;
		this.childIndex = childIndex;
		this.propAddress = null;
	}

	public void updateResolvedValues(LocationResolver locationResolver)
	{
		propAddress = locationResolver.resolveAbsoluteOrNull(new PropertiesLocation(index));
	}

	public void append(SpecialLocationEmitter locationEmitter, SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
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
			memSeq.writeNextWord(bigintIntChecked(16, propAddress, (i) -> String.format("Property location cannot be more than 2 bytes but is: %s", i.toString())));
		}
	}
}
