package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record ConstantByteSequence(List<ConstantByteSequenceElement> entries) implements HeaderValue
{
	public ConstantByteSequence(List<ConstantByteSequenceElement> entries)
	{
		this.entries = List.copyOf(entries);
	}
}
