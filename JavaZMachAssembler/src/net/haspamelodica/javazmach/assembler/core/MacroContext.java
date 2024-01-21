package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.integralValueOrNull;

import java.math.BigInteger;
import java.util.Map;

import net.haspamelodica.javazmach.assembler.model.IntegralMacroArgument;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.LabelReferenceMacroArgument;
import net.haspamelodica.javazmach.assembler.model.MacroArgument;
import net.haspamelodica.javazmach.assembler.model.MacroParam;
import net.haspamelodica.javazmach.assembler.model.MacroParamLabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.MacroParamRef;
import net.haspamelodica.javazmach.assembler.model.Operand;
import net.haspamelodica.javazmach.assembler.model.ResolvedMacroArgument;
import net.haspamelodica.javazmach.assembler.model.ResolvedOperand;
import net.haspamelodica.javazmach.assembler.model.StoreTarget;
import net.haspamelodica.javazmach.assembler.model.Variable;

public record MacroContext(int refId, Map<String, ResolvedMacroArgumentWithContext> args, MacroContext outerMacroContext)
{
	public static final MacroContext GLOBAL_MACRO_CONTEXT = new MacroContext(0, Map.of(), null);
	public static final int FIRST_NONGLOBAL_MACRO_REFID = 1;

	public MacroContext(int refId, Map<String, ResolvedMacroArgumentWithContext> args, MacroContext outerMacroContext)
	{
		this.refId = refId;
		this.args = Map.copyOf(args);
		this.outerMacroContext = outerMacroContext;
	}

	public BigInteger resolveLabelRef(String labelName, ValueReferenceResolver valueReferenceResolver)
	{
		for(MacroContext context = this; context != null; context = context.outerMacroContext())
		{
			BigInteger resolved = valueReferenceResolver.resolveAbsoluteOrNull(new LabelLocation(context.refId(), labelName));
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
			case Variable a -> defaultError("Macro param used in integral expression was a variable: " + param.name());
			// Use outerMacroContext instead of this:
			// If a macro with param a calls another macro, giving it its param a as the argument to the inner macro's param a,
			// then the inner macro's a should resolve to the value of the outer macro's a.
			//TODO this is not entirely clean. The route used with Operand / ResolvedOperand would be cleaner:
			// introduce a ResolvedIntegralValue and let a ResolvedOperand not permit IntegralValue, but ResolvedIntegralValue.
			//TODO not correct - see test.zasm
			case IntegralMacroArgument a -> integralValueOrNull(outerMacroContext(), ival(a), valueReferenceResolver);
		};
	}

	public AssembledLabelDeclaration resolveAssembledLabelDeclaration(MacroParamLabelDeclaration labelDeclaration)
	{
		MacroParam param = labelDeclaration.param();
		ResolvedMacroArgumentWithContext resolved = resolveMacroParamWithContext(param);
		return switch(resolved.resolvedArgument())
		{
			case Variable a -> defaultError("Macro param used in label declaration was a variable: " + param.name());
			case IntegralMacroArgument arg -> switch(arg)
			{
				case IntegralValue a -> defaultError("Macro param used in label declaration was an IntegralValue: " + param.name());
				// ensure the label is declared in the context which actually provided the argument
				case LabelReferenceMacroArgument a -> new AssembledLabelDeclaration(resolved.macroContext(), a.labelReference().name());
			};
		};
	}

	public Variable resolveStoreTarget(StoreTarget storeTarget)
	{
		return switch(storeTarget)
		{
			case Variable a -> a;
			case MacroParam param -> switch(resolveMacroParam(param))
			{
				case Variable a -> a;
				case IntegralMacroArgument a -> defaultError("Macro param used as store target was an integral value: " + param.name() + ": " + a);
			};
		};
	}

	public ResolvedOperand resolveOperand(Operand operand)
	{
		return switch(operand)
		{
			case ResolvedOperand r -> r;
			case MacroParam param -> switch(resolveMacroParam(param))
			{
				case Variable a -> a;
				case IntegralMacroArgument a -> ival(a);
			};
		};
	}

	public ResolvedMacroArgumentWithContext resolveMacroArgument(MacroArgument argument)
	{
		return switch(argument)
		{
			case ResolvedMacroArgument a -> new ResolvedMacroArgumentWithContext(this, a);
			case MacroParam param -> resolveMacroParamWithContext(param);
		};
	}

	private IntegralValue ival(IntegralMacroArgument arg)
	{
		return switch(arg)
		{
			case IntegralValue a -> a;
			case LabelReferenceMacroArgument a -> a.labelReference();
		};
	}

	public ResolvedMacroArgument resolveMacroParam(MacroParam param)
	{
		return resolveMacroParamWithContext(param).resolvedArgument();
	}

	public ResolvedMacroArgumentWithContext resolveMacroParamWithContext(MacroParam param)
	{
		ResolvedMacroArgumentWithContext resolved = args.get(param.name());
		if(resolved == null)
			defaultError("Unknown macro parameter: " + param.name());
		return resolved;
	}

	public static record ResolvedMacroArgumentWithContext(MacroContext macroContext, ResolvedMacroArgument resolvedArgument)
	{}
}
