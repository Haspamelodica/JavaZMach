package net.haspamelodica.javaz;

import net.haspamelodica.javaz.core.stack.CallStack;

public class TestCallStack
{
	public static void main(String[] args)
	{
		//TODO test
		System.out.println("initial:");
		CallStack stack = new CallStack();
		System.out.println(stack);
		System.out.println("push 0x123:");
		stack.push(0x123);
		System.out.println(stack);
		System.out.println("push 0x456:");
		stack.push(0x456);
		System.out.println(stack);
		System.out.println("push call frame:");
		stack.pushCallFrame(0x1234, 5, new int[] {1, 2, 3, 4, 5}, 2, true, 34);
		System.out.println(stack);
		System.out.println("pop call frame:");
		stack.popCallFrame();
		System.out.println(stack);
		System.out.println("pop:");
		stack.pop();
		System.out.println(stack);
		System.out.println("push call frame:");
		stack.pushCallFrame(0x1234, 6, new int[] {10, 20, 30, 40, 50, 60}, 2, true, 34);
		System.out.println(stack);
		System.out.println(stack.getCurrentCallFrameDiscardReturnValue());
		System.out.println(stack.getCurrentCallFrameFP());
		System.out.println(stack.getCurrentCallFrameLocalVariableCount());
		System.out.println(stack.getCurrentCallFrameStoreTarget());
		System.out.println(stack.readLocalVariable(1));
		System.out.println(stack.readLocalVariable(4));
	}
}