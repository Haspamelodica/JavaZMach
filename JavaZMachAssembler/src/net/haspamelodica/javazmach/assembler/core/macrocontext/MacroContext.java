package net.haspamelodica.javazmach.assembler.core.macrocontext;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;

import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledLabelDeclaration;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedBinaryExpression;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralLiteral;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralValue;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReference;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReferenceIntegralOnly;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReferenceVariableOnly;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedMacroArgument;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedOperand;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedUnaryExpression;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedVariable;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedVariableConstant;
import net.haspamelodica.javazmach.assembler.core.valuereferences.LabelLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.core.valuereferences.value.ReferredValue;
import net.haspamelodica.javazmach.assembler.model.entries.MacroParamLabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.values.BinaryExpression;
import net.haspamelodica.javazmach.assembler.model.values.IntegralLiteral;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.values.LabelReference;
import net.haspamelodica.javazmach.assembler.model.values.LabelReferenceIntegralOnly;
import net.haspamelodica.javazmach.assembler.model.values.LabelReferenceMacroArgument;
import net.haspamelodica.javazmach.assembler.model.values.LabelReferenceVariableOnly;
import net.haspamelodica.javazmach.assembler.model.values.MacroArgument;
import net.haspamelodica.javazmach.assembler.model.values.MacroParam;
import net.haspamelodica.javazmach.assembler.model.values.MacroParamRef;
import net.haspamelodica.javazmach.assembler.model.values.Operand;
import net.haspamelodica.javazmach.assembler.model.values.StoreTarget;
import net.haspamelodica.javazmach.assembler.model.values.UnaryExpression;
import net.haspamelodica.javazmach.assembler.model.values.Variable;

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

	public ReferredValue resolveLabel(String labelName, ValueReferenceResolver valueReferenceResolver)
	{
		return resolveLabelGeneric(labelName, valueReferenceResolver::resolveAbsoluteOrNull, valueReferenceResolver::tryResolveAbsoluteOrNull);
	}
	public BigInteger resolveLabelIntegral(String labelName, ValueReferenceResolver valueReferenceResolver)
	{
		return resolveLabelGeneric(labelName, valueReferenceResolver::resolveAbsoluteOrNullIntegral, valueReferenceResolver::tryResolveAbsoluteOrNullIntegral);
	}
	public Variable resolveLabelVariable(String labelName, ValueReferenceResolver valueReferenceResolver)
	{
		return resolveLabelGeneric(labelName, valueReferenceResolver::resolveAbsoluteOrNullVariable, valueReferenceResolver::tryResolveAbsoluteOrNullVariable);
	}

	private <R> R resolveLabelGeneric(String labelName, Function<LabelLocation, R> resolveLabel, Function<LabelLocation, R> tryResolveLabel)
	{
		for(MacroContext context = this; context != null; context = context.outerMacroContext())
		{
			R resolved = tryResolveLabel.apply(new LabelLocation(context.refId(), labelName));
			if(resolved != null)
				return resolved;
		}
		// Label can't be found. This'll return null, but also emit the correct error diagnostic.
		// This makes future error messages less confusing.
		return resolveLabel.apply(new LabelLocation(refId(), labelName));
	}

	public AssembledLabelDeclaration resolveAssembledLabelDeclaration(MacroParamLabelDeclaration labelDeclaration)
	{
		MacroParam param = labelDeclaration.param();
		ResolvedMacroArgumentWithContext resolved = resolveWithContext(param);
		return switch(resolved.resolvedArgument())
		{
			case ResolvedIntegralValue a -> defaultError("Macro param used in label declaration was an IntegralValue: " + param.name());
			case ResolvedLabelReference a -> new AssembledLabelDeclaration(a.macroContext(), a.name());
			case ResolvedVariable a -> defaultError("Macro param used in label declaration was a variable: " + param.name());
		};
	}

	public ResolvedVariable resolve(StoreTarget storeTarget)
	{
		return switch(storeTarget)
		{
			case Variable s -> new ResolvedVariableConstant(s);
			case LabelReferenceVariableOnly o -> new ResolvedLabelReferenceVariableOnly(this, o.name());
			case MacroParam param -> resolveParamVariable(param);
		};
	}

	public ResolvedOperand resolve(Operand operand)
	{
		return switch(operand)
		{
			case IntegralValue o -> resolve(o);
			case Variable o -> new ResolvedVariableConstant(o);
			case LabelReference o -> new ResolvedLabelReference(this, o.name());
			case MacroParam param -> switch(resolve(param))
			{
				case ResolvedIntegralValue o -> o;
				case ResolvedVariable o -> o;
				case ResolvedLabelReference o -> o;
			};
		};
	}

	public ResolvedMacroArgumentWithContext resolveWithContext(MacroArgument argument)
	{
		return switch(argument)
		{
			case IntegralValue a -> new ResolvedMacroArgumentWithContext(this, resolve(a));
			case Variable a -> new ResolvedMacroArgumentWithContext(this, new ResolvedVariableConstant(a));
			case LabelReferenceMacroArgument a -> new ResolvedMacroArgumentWithContext(this, new ResolvedLabelReference(this, a.name()));
			case MacroParam a -> resolveWithContext(a);
		};
	}

	public ResolvedIntegralValue resolve(IntegralValue integralValue)
	{
		return switch(integralValue)
		{
			case IntegralLiteral v -> new ResolvedIntegralLiteral(v);
			case LabelReferenceIntegralOnly v -> new ResolvedLabelReferenceIntegralOnly(this, v.name());
			case BinaryExpression v -> new ResolvedBinaryExpression(resolve(v.lhs()), v.op(), resolve(v.rhs()));
			case UnaryExpression v -> new ResolvedUnaryExpression(v.op(), resolve(v.operand()));
			case MacroParamRef v -> resolveParamIntegral(v.param());
		};
	}

	public ResolvedIntegralValue resolveParamIntegral(MacroParam param)
	{
		return switch(resolve(param))
		{
			case ResolvedIntegralValue v -> v;
			case ResolvedVariable v -> defaultError("Macro param used as integral value was a variable: "
					+ param.name() + ": " + v);
			case ResolvedLabelReference v -> new ResolvedLabelReferenceIntegralOnly(v.macroContext(), v.name());
		};
	}

	public ResolvedVariable resolveParamVariable(MacroParam param)
	{
		return switch(resolve(param))
		{
			case ResolvedIntegralValue s -> defaultError("Macro param used as store target was an integral value: "
					+ param.name() + ": " + s);
			case ResolvedVariable s -> s;
			case ResolvedLabelReference s -> new ResolvedLabelReferenceVariableOnly(s.macroContext(), s.name());
		};
	}

	public ResolvedMacroArgument resolve(MacroParam param)
	{
		return resolveWithContext(param).resolvedArgument();
	}

	public ResolvedMacroArgumentWithContext resolveWithContext(MacroParam param)
	{
		ResolvedMacroArgumentWithContext resolved = args.get(param.name());
		if(resolved == null)
			defaultError("Unknown macro parameter: " + param.name());
		return resolved;
	}

	public static record ResolvedMacroArgumentWithContext(MacroContext macroContext, ResolvedMacroArgument resolvedArgument)
	{}
}
