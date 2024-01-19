package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.math.BigInteger;
import java.util.Map;

import net.haspamelodica.javazmach.assembler.model.LabelReference;
import net.haspamelodica.javazmach.assembler.model.MacroParam;
import net.haspamelodica.javazmach.assembler.model.Operand;
import net.haspamelodica.javazmach.assembler.model.ResolvedOperand;

public record MacroContext(int refId, Map<String, ResolvedOperand> args, MacroContext outerMacroContext)
{
	public static final MacroContext GLOBAL_MACRO_CONTEXT = new MacroContext(0, Map.of(), null);
	public static final int FIRST_NONGLOBAL_MACRO_REFID = 1;

	public MacroContext(int refId, Map<String, ResolvedOperand> args, MacroContext outerMacroContext)
	{
		this.refId = refId;
		this.args = Map.copyOf(args);
		this.outerMacroContext = outerMacroContext;
	}

	public BigInteger resolve(LabelReference labelRef, ValueReferenceResolver valueReferenceResolver)
	{
		for(MacroContext context = this; context != null; context = context.outerMacroContext())
		{
			BigInteger resolved = valueReferenceResolver.resolveAbsoluteOrNull(new LabelLocation(context.refId(), labelRef.name()));
			if(resolved != null)
				return resolved;
		}
		return null;
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
