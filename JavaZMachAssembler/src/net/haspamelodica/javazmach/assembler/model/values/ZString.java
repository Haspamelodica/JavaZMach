package net.haspamelodica.javazmach.assembler.model.values;

import java.util.List;

import net.haspamelodica.javazmach.assembler.model.values.zstrings.ZStringElement;

public record ZString(List<ZStringElement> elements) implements ByteSequenceElement
{
	public ZString(List<ZStringElement> elements)
	{
		this.elements = List.copyOf(elements);
	}
}
