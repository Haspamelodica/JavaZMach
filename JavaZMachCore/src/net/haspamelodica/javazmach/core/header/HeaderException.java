package net.haspamelodica.javazmach.core.header;

import net.haspamelodica.javazmach.core.InterpreterException;

public class HeaderException extends InterpreterException
{
	public HeaderException()
	{
		super();
	}
	protected HeaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public HeaderException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public HeaderException(String message)
	{
		super(message);
	}
	public HeaderException(Throwable cause)
	{
		super(cause);
	}
}