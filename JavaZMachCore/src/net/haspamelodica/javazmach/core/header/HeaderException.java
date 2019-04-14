package net.haspamelodica.javazmach.core.header;

public class HeaderException extends RuntimeException
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