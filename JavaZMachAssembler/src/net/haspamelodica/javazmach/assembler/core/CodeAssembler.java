package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.DiagnosticHandler.defaultEmit;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class CodeAssembler implements LocationResolver
{
	private final List<AssembledInstruction>	code;
	private final NoRangeCheckMemory			codeMem;
	private final SequentialMemoryWriteAccess	codeSeq;
	private final Map<String, Location>			labelLocations;
	private final Map<Section, BigInteger>	sectionLocations;
	private final int codeStart;

	private final Map<AssembledInstruction, BigInteger>	instructionBranchOriginLocations;
	private final Map<AssembledInstruction, BigInteger>	instructionEndLocations;
	
	public CodeAssembler(List<AssembledInstruction> code, NoRangeCheckMemory codeMem, SequentialMemoryWriteAccess codeSeq,
			Map<String, Location> labelLocations, int codeStart)
	{
		this.code = code;
		this.codeMem = codeMem;
		this.codeSeq = codeSeq;
		this.labelLocations = labelLocations;
		this.codeStart = codeStart;
		
		this.sectionLocations = new HashMap<>();
		this.instructionBranchOriginLocations = new HashMap<>();
		this.instructionEndLocations = new HashMap<>();
	}

	public void assembleUntilConvergence()
	{
		List<Diagnostic> diagnostics;
		for(;;)
		{
			diagnostics = new ArrayList<>();
			DiagnosticHandler diagnosticHandler = diagnostics::add;
			if(assembleOneIteration(diagnosticHandler))
				break;

			// If we get here, we haven't converged yet.
			// Implicitly discard all diagnostics; even errors might disappear later.
			// Also don't forget to reset codeMem and codeSeq.
			codeSeq.setAddress(0);
			codeMem.clear();
		}

		// Convergence reached! Now handle all diagnostics.
		//TODO stack trace is lost this way - maybe record in list of diagnostics if this is worth the effort?
		for(Diagnostic diagnostic : diagnostics)
			defaultEmit(diagnostic);
	}

	private boolean assembleOneIteration(DiagnosticHandler diagnosticHandler)
	{
		boolean noChangeSoFar = true;

		for(AssembledInstruction instruction : code)
		{
			instruction.updateResolvedValues(this);

			instruction.appendUntilBranchOrigin(codeSeq, diagnosticHandler);
			noChangeSoFar &= updateInstructionLocation(instructionBranchOriginLocations, instruction, codeSeq.getAddress());
			instruction.appendAfterBranchOrigin(codeSeq, diagnosticHandler);
			noChangeSoFar &= updateInstructionLocation(instructionEndLocations, instruction, codeSeq.getAddress());
		}

		return noChangeSoFar;
	}

	private boolean updateInstructionLocation(Map<AssembledInstruction, BigInteger> instructionLocations,
			AssembledInstruction instruction, int reladdr)
	{
		BigInteger newLocation = BigInteger.valueOf(codeStart + reladdr);
		BigInteger oldLocation = instructionLocations.put(instruction, newLocation);
		return newLocation.equals(oldLocation);
	}

	@Override
	public BigInteger locationAbsoluteAddressOrNull(Location location)
	{
		return switch(location)
		{
			case SimpleLocation simpleLocation -> switch(simpleLocation)
			{
				case CODE_START -> BigInteger.valueOf(codeStart);
			};
			case Section section -> {
				yield sectionLocations.get(section);
			}
			case CodeLocation codeLocation -> switch(codeLocation.targetPart())
			{
				case BRANCH_ORIGIN -> instructionBranchOriginLocations.get(codeLocation.instruction());
				case AFTER -> instructionEndLocations.get(codeLocation.instruction());
			};
			case LabelLocation labelLocation ->
			{
				Location resolvedLocation = labelLocations.get(labelLocation.name());
				if(resolvedLocation == null)
					// no need to go through custom DiagnosticHandler: won't change in later iterations
					DiagnosticHandler.defaultError("Label " + labelLocation.name() + " is not defined");
				yield locationAbsoluteAddressOrNull(resolvedLocation);
			}
		};
	}
}
