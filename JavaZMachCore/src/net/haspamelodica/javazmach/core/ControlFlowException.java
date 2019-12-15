package net.haspamelodica.javazmach.core;

public class ControlFlowException extends InterpreterException
{
	private static final long serialVersionUID = -1144596256865959693L;

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