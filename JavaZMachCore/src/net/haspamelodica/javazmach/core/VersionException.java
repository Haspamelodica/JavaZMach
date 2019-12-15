package net.haspamelodica.javazmach.core;

public class VersionException extends InterpreterException
{
	private static final long serialVersionUID = 949917308394344329L;

	public VersionException()
	{
		super();
	}
	protected VersionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public VersionException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public VersionException(String message)
	{
		super(message);
	}
	public VersionException(Throwable cause)
	{
		super(cause);
	}
}