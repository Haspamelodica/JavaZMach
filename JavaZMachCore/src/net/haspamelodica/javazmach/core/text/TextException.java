package net.haspamelodica.javazmach.core.text;

import net.haspamelodica.javazmach.core.InterpreterException;

public class TextException extends InterpreterException
{
	private static final long serialVersionUID = -1262018213776924123L;

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