package net.haspamelodica.javazmach.assembler.model.values;

import java.util.List;

public record ByteSequence(List<ByteSequenceElement> elements) implements HeaderValue
{
	public ByteSequence(List<ByteSequenceElement> elements)
	{
		this.elements = List.copyOf(elements);
	}
}
