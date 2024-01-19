package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.util.Map;

import net.haspamelodica.javazmach.assembler.model.MacroParam;
import net.haspamelodica.javazmach.assembler.model.Operand;
import net.haspamelodica.javazmach.assembler.model.ResolvedOperand;

public record MacroContext(int referenceIdent, Map<String, ResolvedOperand> args)
{
	public MacroContext(int referenceIdent, Map<String, ResolvedOperand> args)
	{
		this.referenceIdent = referenceIdent;
		this.args = Map.copyOf(args);
	}

	public ResolvedOperand resolve(Operand operand)
	{
		return switch(operand)
		{
			case ResolvedOperand r -> r;
			case MacroParam param ->
			{
				ResolvedOperand resolvedOperand = args.get(param.name());
				if(resolvedOperand == null)
					defaultError("Unknown macro parameter: " + param.name());
				yield resolvedOperand;
			}
		};
	}
}
