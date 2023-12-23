package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record ZString(List<ZStringElement> elements)
{
	public ZString(List<ZStringElement> elements)
	{
		this.elements = List.copyOf(elements);
	}
}
