package net.haspamelodica.javaz.model;

public class ControlFlowException extends RuntimeException
{
	public ControlFlowException()
	{
		super();
	}
	protected ControlFlowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public ControlFlowException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public ControlFlowException(String message)
	{
		super(message);
	}
	public ControlFlowException(Throwable cause)
	{
		super(cause);
	}
}