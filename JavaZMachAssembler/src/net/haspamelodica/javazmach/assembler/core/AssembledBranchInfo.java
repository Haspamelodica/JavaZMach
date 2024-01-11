package net.haspamelodica.javazmach.assembler.core;

import net.haspamelodica.javazmach.assembler.model.BranchInfo;
import net.haspamelodica.javazmach.assembler.model.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.SimpleBranchTarget;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;

public class AssembledBranchInfo
{
	private final boolean				branchOnConditionFalse;
	private final AssembledBranchTarget	target;

	public AssembledBranchInfo(BranchInfo branchInfo, ValueReference branchOriginLocation)
	{
		this.branchOnConditionFalse = branchInfo.branchOnConditionFalse();
		this.target = switch(branchInfo.target())
		{
			case SimpleBranchTarget target -> new AssembledSimpleBranchTarget(target, branchInfo.branchLengthOverride());
			case IntegralValue target -> new AssembledRegularBranchTarget(target, branchOriginLocation, branchInfo.branchLengthOverride());
		};
	}

	public void updateResolvedTarget(ValueReferenceResolver valueReferenceResolver)
	{
		target.updateResolvedEncodedOffset(valueReferenceResolver);
	}

	public void appendChecked(SequentialMemoryWriteAccess memSeq, DiagnosticHandler diagnosticHandler)
	{
		int encodedOffset = target.getEncodedOffsetChecked(diagnosticHandler).intValue();
		boolean isShort = switch(target.targetLength())
		{
			case SHORTBRANCH -> true;
			case LONGBRANCH -> false;
		};

		memSeq.writeNextByte(0
				// branch-on-condition-false: bit 7; on false is 0, on true is 1.
				| ((branchOnConditionFalse ? 0 : 1) << 7)
				// branch offset encoding: bit 6; long is 0, short is 1.
				| ((isShort ? 1 : 0) << 6)
				// offset / upper byte of offset: bits 5-0
				| (encodedOffset >> (isShort ? 0 : 8)));
		if(!isShort)
			memSeq.writeNextByte(encodedOffset & 0xff);
	}
}
