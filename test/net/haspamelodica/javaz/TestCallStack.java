package net.haspamelodica.javaz;

public class TestCallStack
{
	public static void main(String[] args)
	{
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
		stack.pushCallFrame(0x1234, 5, new int[] {1, 2, 3, 4, 5}, 2, true, 34);
		System.out.println(stack);
	}
}