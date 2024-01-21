package net.haspamelodica.javazmach.assembler.core.macrocontext;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultError;

import java.math.BigInteger;
import java.util.Map;

import net.haspamelodica.javazmach.assembler.core.assembledentries.AssembledLabelDeclaration;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedBinaryExpression;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralLiteral;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralMacroArgument;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedIntegralValue;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReference;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedLabelReferenceMacroArgument;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedMacroArgument;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedOperand;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedUnaryExpression;
import net.haspamelodica.javazmach.assembler.core.macrocontext.resolvedvalues.ResolvedVariable;
import net.haspamelodica.javazmach.assembler.core.valuereferences.LabelLocation;
import net.haspamelodica.javazmach.assembler.core.valuereferences.manager.ValueReferenceResolver;
import net.haspamelodica.javazmach.assembler.model.entries.MacroParamLabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.values.BinaryExpression;
import net.haspamelodica.javazmach.assembler.model.values.IntegralLiteral;
import net.haspamelodica.javazmach.assembler.model.values.IntegralMacroArgument;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.values.LabelReference;
import net.haspamelodica.javazmach.assembler.model.values.LabelReferenceMacroArgument;
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

	public BigInteger resolveLabelRef(String labelName, ValueReferenceResolver valueReferenceResolver)
	{
		for(MacroContext context = this; context != null; context = context.outerMacroContext())
		{
			BigInteger resolved = valueReferenceResolver.tryResolveAbsoluteOrNull(new LabelLocation(context.refId(), labelName));
			if(resolved != null)
				return resolved;
		}
		// Label can't be found. This'll return null, but also emit the correct error diagnostic.
		// This makes future error messages less confusing.
		return valueReferenceResolver.resolveAbsoluteOrNull(new LabelLocation(refId(), labelName));
	}

	public AssembledLabelDeclaration resolveAssembledLabelDeclaration(MacroParamLabelDeclaration labelDeclaration)
	{
		MacroParam param = labelDeclaration.param();
		ResolvedMacroArgumentWithContext resolved = resolveWithContext(param);
		return switch(resolved.resolvedArgument())
		{
			case ResolvedVariable a -> defaultError("Macro param used in label declaration was a variable: " + param.name());
			case ResolvedIntegralMacroArgument arg -> switch(arg)
			{
				case ResolvedIntegralValue a -> defaultError("Macro param used in label declaration was an IntegralValue: " + param.name());
				case ResolvedLabelReferenceMacroArgument a -> new AssembledLabelDeclaration(a.labelReference().macroContext(), a.labelReference().name());
			};
		};
	}

	public Variable resolveStoreTarget(StoreTarget storeTarget)
	{
		return switch(storeTarget)
		{
			case Variable a -> a;
			case MacroParam param -> switch(resolve(param))
			{
				case ResolvedVariable a -> a.variable();
				case ResolvedIntegralMacroArgument a -> defaultError("Macro param used as store target was an integral value: "
						+ param.name() + ": " + a);
			};
		};
	}

	public ResolvedOperand resolve(Operand operand)
	{
		return switch(operand)
		{
			case IntegralValue o -> resolve(o);
			case Variable o -> new ResolvedVariable(o);
			case MacroParam param -> switch(resolve(param))
			{
				case ResolvedIntegralMacroArgument o -> ival(o);
				case ResolvedVariable o -> o;
			};
		};
	}

	public ResolvedMacroArgumentWithContext resolveWithContext(MacroArgument argument)
	{
		return switch(argument)
		{
			case IntegralMacroArgument a -> new ResolvedMacroArgumentWithContext(this, resolve(a));
			case Variable a -> new ResolvedMacroArgumentWithContext(this, new ResolvedVariable(a));
			case MacroParam a -> resolveWithContext(a);
		};
	}

	public ResolvedIntegralMacroArgument resolve(IntegralMacroArgument argument)
	{
		return switch(argument)
		{
			case IntegralValue a -> resolve(a);
			case LabelReferenceMacroArgument a -> new ResolvedLabelReferenceMacroArgument(new ResolvedLabelReference(this, a.labelReference().name()));
		};
	}

	public ResolvedIntegralValue resolve(IntegralValue integralValue)
	{
		return switch(integralValue)
		{
			case IntegralLiteral v -> new ResolvedIntegralLiteral(v);
			case LabelReference v -> new ResolvedLabelReference(this, v.name());
			case BinaryExpression v -> new ResolvedBinaryExpression(resolve(v.lhs()), v.op(), resolve(v.rhs()));
			case UnaryExpression v -> new ResolvedUnaryExpression(v.op(), resolve(v.operand()));
			case MacroParamRef v -> resolveParamIntegral(v.param());
		};
	}

	public ResolvedIntegralValue resolveParamIntegral(MacroParam param)
	{
		return switch(resolve(param))
		{
			case ResolvedVariable v -> defaultError("Macro param used as integral value was a variable: "
					+ param.name() + ": " + v);
			case ResolvedIntegralMacroArgument value -> ival(value);
		};
	}

	private ResolvedIntegralValue ival(ResolvedIntegralMacroArgument value)
	{
		return switch(value)
		{
			case ResolvedIntegralValue v -> v;
			case ResolvedLabelReferenceMacroArgument v -> v.labelReference();
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
