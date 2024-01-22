package net.haspamelodica.javazmach.assembler.core.valuereferences.manager;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.core.valuereferences.ValueReference;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.IntegralReferredValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.ReferredValue;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.VariableReferredValue;
import net.haspamelodica.javazmach.assembler.model.values.Variable;

public abstract class SimpleValueReferenceResolver implements ValueReferenceResolver
{
	protected abstract <R> R emitErrorOrNull(String message);

	@Override
	public BigInteger resolveAbsoluteOrNullIntegral(ValueReference location)
	{
		return checkedIntegral(resolveAbsoluteOrNull(location), location);
	}

	@Override
	public BigInteger tryResolveAbsoluteOrNullIntegral(ValueReference location)
	{
		return checkedIntegral(tryResolveAbsoluteOrNull(location), location);
	}

	@Override
	public Variable resolveAbsoluteOrNullVariable(ValueReference location)
	{
		return checkedVariable(resolveAbsoluteOrNull(location), location);
	}

	@Override
	public Variable tryResolveAbsoluteOrNullVariable(ValueReference location)
	{
		return checkedVariable(tryResolveAbsoluteOrNull(location), location);
	}

	private BigInteger checkedIntegral(ReferredValue value, ValueReference locationForDiagnostic)
	{
		return switch(value)
		{
			case null -> null;
			case IntegralReferredValue r -> r.value();
			case VariableReferredValue r -> emitErrorOrNull("Reference resolving to a variable used as an integral value: " + locationForDiagnostic);
		};
	}

	private Variable checkedVariable(ReferredValue value, ValueReference locationForDiagnostic)
	{
		return switch(value)
		{
			case null -> null;
			case IntegralReferredValue r -> emitErrorOrNull("Reference resolving to an integral value used as a variable: " + locationForDiagnostic);
			case VariableReferredValue r -> r.value();
		};
	}
}
