package net.haspamelodica.javaz.core.io;

public class IOException extends RuntimeException
{
	public IOException()
	{
		super();
	}
	protected IOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public IOException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public IOException(String message)
	{
		super(message);
	}
	public IOException(Throwable cause)
	{
		super(cause);
	}
}