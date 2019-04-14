package net.haspamelodica.javazmach.core.stack;

import net.haspamelodica.javazmach.core.VariableException;

public class CallStack
{
	private final FramedStack stack;

	public CallStack()
	{
		stack = new FramedStack();
	}

	public void reset()
	{
		stack.reset();
	}

	public int pop()
	{
		return stack.pop();
	}
	public void push(int val)
	{
		stack.push(val);
	}
	/*
	 * Call frame format:
	 * fp-4   MSWord of return PC
	 * fp-3   LSWord of return PC
	 * fp-2   aaap vvvv  ssss ssss:   a: supplied argument count, p: return value discarded, v: local variable count, s: store target (variable number)
	 * fp-1   MSWord of return FP
	 * fp     LSWord of return FP
	 * fp+1   1st local variable
	 * fp+2   2nd local variable
	 * ...
	 * fp+v+1 first "evaluation stack" entry for current routine
	 */
	public void pushCallFrame(int returnPC, int variablesCount, int[] variablesInitialValues, int suppliedArgumentCount, boolean discardReturnValue, int storeTarget)
	{
		if(variablesCount >>> 4 != 0)//only the lower 4 bit are allowed to be set
			throw new VariableException("Illegal variable count: " + variablesCount);
		int returnFP = stack.getFP();

		stack.push(returnPC >>> 16);
		stack.push(returnPC & 0xFFFF);
		stack.push((suppliedArgumentCount << 13) | (discardReturnValue ? 0x10_00 : 0x00_00) | (variablesCount << 8) | storeTarget);
		stack.push(returnFP >>> 16);
		stack.setFPSPRelative(0);
		stack.push(returnFP & 0xFFFF);
		for(int i = 0; i < variablesCount; i ++)
			stack.push(variablesInitialValues[i]);
	}
	public boolean getCurrentCallFrameDiscardReturnValue()
	{
		return (stack.readFPRelative(-2) & 0x10_00) != 0;
	}
	public int getCurrentCallFrameLocalVariableCount()
	{
		return (stack.readFPRelative(-2) & 0x0F_00) >>> 8;
	}
	public int getCurrentCallFrameSuppliedArgumentsCount()
	{
		return (stack.readFPRelative(-2) & 0xE0_00) >>> 13;
	}
	public int getCurrentCallFrameStoreTarget()
	{
		return stack.readFPRelative(-2) & 0x00_FF;
	}
	/**
	 * When no frame is on this stack, returns -1.
	 */
	public int getCurrentCallFrameFP()
	{
		return stack.getFP();
	}
	public int readLocalVariable(int var)
	{
		checkLocalVariable(var);
		return stack.readFPRelative(var);
	}
	public void writeLocalVariable(int var, int val)
	{
		checkLocalVariable(var);
		stack.writeFPRelative(var, val);
	}
	private void checkLocalVariable(int var)
	{
		if(var < 1 || var > getCurrentCallFrameLocalVariableCount())
			throw new VariableException("Illegal local variable: " + var);
	}
	/**
	 * Returns <code>returnPC</code>.
	 */
	public int popCallFrame()
	{
		int oldFP = stack.getFP();
		int returnPC = (stack.readFPRelative(-4) << 16) | stack.readFPRelative(-3);
		int newFP = (stack.readFPRelative(-1) << 16) | stack.readFPRelative(0);

		stack.setFP(newFP);
		stack.setSP(oldFP - 4);
		return returnPC;
	}

	@Override
	public String toString()
	{
		return stack.toString();
	}
}