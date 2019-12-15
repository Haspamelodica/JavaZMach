package net.haspamelodica.javazmach.core;

public class ArithmeticException extends InterpreterException
{
	private static final long serialVersionUID = -4693970375043059175L;

	public ArithmeticException()
	{
		super();
	}
	protected ArithmeticException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public ArithmeticException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public ArithmeticException(String message)
	{
		super(message);
	}
	public ArithmeticException(Throwable cause)
	{
		super(cause);
	}
}