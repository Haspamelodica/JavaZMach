package net.haspamelodica.javaz.model.instructions;

public class InstructionFormatException extends RuntimeException
{
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