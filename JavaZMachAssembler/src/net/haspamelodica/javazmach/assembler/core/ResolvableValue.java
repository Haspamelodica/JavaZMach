package net.haspamelodica.javazmach.assembler.core;

import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;

public class ResolvableValue<V>
{
	private final Function<ValueReferenceResolver, V>	value;
	private final V										defaultValue;

	private V resolvedValue;

	public ResolvableValue(Function<ValueReferenceResolver, V> value, V defaultValue)
	{
		this.value = value;
		this.defaultValue = defaultValue;
	}

	public void updateResolvedValue(ValueReferenceResolver valueReferenceResolver)
	{
		resolvedValue = value.apply(valueReferenceResolver);
	}

	public V resolvedValueOrNull()
	{
		return resolvedValue;
	}
	public V resolvedValueOrDefault()
	{
		return resolvedValue == null ? defaultValue : resolvedValue;
	}
}
