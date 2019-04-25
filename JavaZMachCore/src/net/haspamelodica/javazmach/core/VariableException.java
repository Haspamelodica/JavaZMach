package net.haspamelodica.javazmach.core;

public class VariableException extends InterpreterException
{
	public VariableException()
	{
		super();
	}
	protected VariableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public VariableException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public VariableException(String message)
	{
		super(message);
	}
	public VariableException(Throwable cause)
	{
		super(cause);
	}
}