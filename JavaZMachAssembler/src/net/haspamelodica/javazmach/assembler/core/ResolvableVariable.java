package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.variableOrNull;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedVariable;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedVariableConstant;
import net.haspamelodica.javazmach.assembler.model.values.StackPointer;
import net.haspamelodica.javazmach.assembler.model.values.Variable;

public class ResolvableVariable extends ResolvableValue<Variable>
{
	public ResolvableVariable(ResolvedVariable variable)
	{
		super(resolver -> variableOrNull(variable, resolver), StackPointer.INSTANCE);
	}

	public Variable resolvedVariableOrSp()
	{
		return resolvedValueOrDefault();
	}

	public static ResolvableVariable resolvableVariableOrSp(Optional<ResolvedVariable> variable)
	{
		return new ResolvableVariable(variable.orElse(new ResolvedVariableConstant(StackPointer.INSTANCE)));
	}
}
