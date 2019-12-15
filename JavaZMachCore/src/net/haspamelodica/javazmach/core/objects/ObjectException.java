package net.haspamelodica.javazmach.core.objects;

import net.haspamelodica.javazmach.core.InterpreterException;

public class ObjectException extends InterpreterException
{
	private static final long serialVersionUID = 7101879516435445257L;

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