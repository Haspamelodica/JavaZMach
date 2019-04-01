package net.haspamelodica.javaz.core;

public class ArithmeticException extends RuntimeException
{
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