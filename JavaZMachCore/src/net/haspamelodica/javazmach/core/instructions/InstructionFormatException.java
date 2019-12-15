package net.haspamelodica.javazmach.core.instructions;

import net.haspamelodica.javazmach.core.InterpreterException;

public class InstructionFormatException extends InterpreterException
{
	private static final long serialVersionUID = 5747746791406362758L;

	public InstructionFormatException()
	{
		super();
	}
	protected InstructionFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
	public InstructionFormatException(String message, Throwable cause)
	{
		super(message, cause);
	}
	public InstructionFormatException(String message)
	{
		super(message);
	}
	public InstructionFormatException(Throwable cause)
	{
		super(cause);
	}
}