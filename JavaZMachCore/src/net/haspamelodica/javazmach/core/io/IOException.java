package net.haspamelodica.javazmach.core.io;

import net.haspamelodica.javazmach.core.InterpreterException;

public class IOException extends InterpreterException
{
	private static final long serialVersionUID = -6769578147485699867L;

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