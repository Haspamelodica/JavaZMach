package net.haspamelodica.javazmach.core;

public class InterpreterException extends RuntimeException
{
	public InterpreterException()
	{
		super();
	}
	protected InterpreterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public InterpreterException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public InterpreterException(String message)
	{
		super(message);
	}
	public InterpreterException(Throwable cause)
	{
		super(cause);
	}
}