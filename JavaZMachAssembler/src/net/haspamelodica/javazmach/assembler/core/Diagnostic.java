package net.haspamelodica.javazmach.assembler.core;

public record Diagnostic(Severity severity, String message)
{
	public static enum Severity
	{
		ERROR,
		WARNING,
		INFO;
	}
}
