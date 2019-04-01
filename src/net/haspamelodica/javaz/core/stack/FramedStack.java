package net.haspamelodica.javaz.core.stack;

import java.util.Arrays;

public class FramedStack
{
	private static final int SIZE_OVERHEAD = 10;

	private short[] mem;

	private int	sp;
	private int	fp;

	public FramedStack()
	{
		this.mem = new short[SIZE_OVERHEAD];
		this.fp = -1;
	}

	public int pop()
	{
		checkRW(-- sp);
		return getWordAtUnchecked(sp);
	}
	public void push(int val)
	{
		setWordAt(sp ++, val);
	}
	public int readFPRelative(int off)
	{
		return getWordAt(fp + off);
	}
	public void writeFPRelative(int off, int val)
	{
		setWordAt(fp + off, val);
	}
	public int readAbsolute(int addr)
	{
		return getWordAt(addr);
	}
	public void writeAbsolute(int addr, int val)
	{
		setWordAt(addr, val);
	}

	/**
	 * Equal to the number of words pushed onto the stack.
	 */
	public int getSP()
	{
		return sp;
	}
	public void setSP(int sp)
	{
		this.sp = sp;
	}
	public int getFP()
	{
		return fp;
	}
	public void setFP(int fp)
	{
		this.fp = fp;
	}
	/**
	 * After a call to this method with argument <code>0</code> followed by <code>push(x)</code>,
	 * <code>readFPRelative(0)</code> will return x.
	 */
	public void setFPSPRelative(int spOff)
	{
		fp = sp + spOff;
	}

	private void checkRO(int addr)
	{
		checkRW(addr);
		if(addr >= sp)
			throw new IndexOutOfBoundsException("Stack overflow");
	}
	private void checkRW(int addr)
	{
		if(addr < 0)
			throw new IndexOutOfBoundsException("Stack underflow");
	}
	private int getWordAt(int i)
	{
		checkRO(i);
		growMemTo(i);
		return getWordAtUnchecked(i);
	}
	private int getWordAtUnchecked(int i)
	{
		return mem[i] & 0xFFFF;
	}
	private void setWordAt(int i, int val)
	{
		checkRW(i);
		growMemTo(i);
		mem[i] = (short) val;
	}
	private void growMemTo(int i)
	{
		if(mem.length <= i)
			mem = Arrays.copyOf(mem, i + SIZE_OVERHEAD);
	}

	@Override
	public String toString()
	{
		growMemTo(sp - 1);
		StringBuilder result = new StringBuilder();
		for(int i = sp - 1; i >= 0; i --)
			result.append(String.format("%04x ", mem[i]));
		result.append(System.lineSeparator());
		for(int i = sp - fp; i > 1; i --)
			result.append("     ");
		result.append("^fp^");
		return result.toString();
	}
}