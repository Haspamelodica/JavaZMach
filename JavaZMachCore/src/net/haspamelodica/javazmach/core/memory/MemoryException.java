package net.haspamelodica.javazmach.core.memory;

import net.haspamelodica.javazmach.core.InterpreterException;

public class MemoryException extends InterpreterException
{
	public MemoryException()
	{
		super();
	}
	protected MemoryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public MemoryException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public MemoryException(String message)
	{
		super(message);
	}
	public MemoryException(Throwable cause)
	{
		super(cause);
	}
}