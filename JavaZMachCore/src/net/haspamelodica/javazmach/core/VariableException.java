package net.haspamelodica.javazmach.core;

public class VariableException extends InterpreterException
{
	private static final long serialVersionUID = 8435606980275562238L;

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