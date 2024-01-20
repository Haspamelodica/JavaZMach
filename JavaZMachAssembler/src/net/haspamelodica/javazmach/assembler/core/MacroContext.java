package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValueOrNull;

import java.math.BigInteger;
import java.util.Map;

import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.LabelReference;
import net.haspamelodica.javazmach.assembler.model.MacroParam;
import net.haspamelodica.javazmach.assembler.model.MacroParamRef;
import net.haspamelodica.javazmach.assembler.model.Operand;
import net.haspamelodica.javazmach.assembler.model.ResolvedOperand;
import net.haspamelodica.javazmach.assembler.model.StoreTarget;
import net.haspamelodica.javazmach.assembler.model.Variable;

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

	public BigInteger resolveLabelRef(LabelReference labelRef, ValueReferenceResolver valueReferenceResolver)
	{
		for(MacroContext context = this; context != null; context = context.outerMacroContext())
		{
			BigInteger resolved = valueReferenceResolver.resolveAbsoluteOrNull(new LabelLocation(context.refId(), labelRef.name()));
			if(resolved != null)
				return resolved;
		}
		return null;
	}

	public BigInteger resolveIntegralValue(MacroParamRef paramRef, ValueReferenceResolver valueReferenceResolver)
	{
		MacroParam param = paramRef.param();
		return switch(resolveMacroParam(param))
		{
			case Variable v -> defaultError("Macro param used in integral expression was not an IntegralValue, but a Variable: " + param.name());
			// Use outerMacroContext instead of this:
			// If a macro with param a calls another macro, giving it its param a as the argument to the inner macro's param a,
			// then the inner macro's a should resolve to the value of the outer macro's a.
			//TODO this is not entirely clean. The route used with Operand / ResolvedOperand would be cleaner:
			// introduce a ResolvedIntegralValue and let a ResolvedOperand not permit IntegralValue, but ResolvedIntegralValue.
			case IntegralValue v -> integralValueOrNull(outerMacroContext(), v, valueReferenceResolver);
		};
	}

	public ResolvedOperand resolveOperand(Operand operand)
	{
		return switch(operand)
		{
			case ResolvedOperand r -> r;
			case MacroParam param -> resolveMacroParam(param);
		};
	}

	public Variable resolveStoreTarget(StoreTarget storeTarget)
	{
		return switch(storeTarget)
		{
			case Variable v -> v;
			case MacroParam param -> switch(resolveMacroParam(param))
			{
				case Variable v -> v;
				case IntegralValue v -> defaultError("Macro param used as store target was not a variable, but an IntegralValue: " + param.name());
			};
		};
	}

	public ResolvedOperand resolveMacroParam(MacroParam param)
	{
		ResolvedOperand resolvedOperand = args.get(param.name());
		if(resolvedOperand == null)
			defaultError("Unknown macro parameter: " + param.name());
		return resolvedOperand;
	}
}
