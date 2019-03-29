package net.haspamelodica.javaz.model.text;

public class TextException extends RuntimeException
{
	public TextException()
	{
		super();
	}
	protected TextException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public TextException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public TextException(String message)
	{
		super(message);
	}
	public TextException(Throwable cause)
	{
		super(cause);
	}
}