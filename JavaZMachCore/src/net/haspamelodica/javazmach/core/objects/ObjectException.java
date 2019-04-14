package net.haspamelodica.javazmach.core.objects;

public class ObjectException extends RuntimeException
{
	public ObjectException()
	{
		super();
	}
	protected ObjectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public ObjectException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public ObjectException(String message)
	{
		super(message);
	}
	public ObjectException(Throwable cause)
	{
		super(cause);
	}
}