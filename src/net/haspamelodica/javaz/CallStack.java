package net.haspamelodica.javaz;

public class CallStack
{
	private final FramedStack stack;

	public CallStack()
	{
		stack = new FramedStack();
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
	 * fp-4  MSWord of return PC
	 * fp-3  LSWord of return PC
	 * fp-2  000p vvvv  ssss ssss:   p: return value discarded, v: local variable count, s: store target (variable number)
	 * fp-1  MSWord of return FP
	 * fp    LSWord of return FP
	 * fp+1  1st local variable
	 * fp+2  2nd local variable
	 * ...
	 * fp+v  first "evaluation stack" entry for current routine
	 */
	public void pushCallFrame(int returnPC, int variablesCount, int[] variablesInitialValues, int suppliedArgumentCount,
			boolean discardReturnValue, int storeTarget)
	{
		int returnFP = stack.getFP();

		stack.push(returnPC >>> 16);
		stack.push(returnPC & 0xFFFF);
		stack.push((discardReturnValue ? 0x10_00 : 0x00_00) | (variablesCount << 8) | storeTarget);
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
	public int getCurrentCallLocalVariableCount()
	{
		return (stack.readFPRelative(-2) & 0x0F_00) >>> 8;
	}
	public int getCurrentCallFrameStoreTarget()
	{
		return stack.readFPRelative(-2) & 0x00_FF;
	}
	public int readLocalVariable(int var)
	{
		return stack.readFPRelative(var);
	}
	public void writeLocalVariable(int var, int val)
	{
		stack.writeFPRelative(var, val);
	}
	/**
	 * Returns <code>returnPC.</code>
	 */
	public int popCallFrame()
	{
		int returnPC = (stack.readFPRelative(-4) << 16) | stack.readFPRelative(-3);
		int returnFP = (stack.readFPRelative(-1) << 16) | stack.readFPRelative(0);

		stack.setFP(returnFP);
		return returnPC;
	}
}