package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.Diagnostic.Severity.ERROR;
import static net.haspamelodica.javazmach.assembler.core.Diagnostic.Severity.INFO;
import static net.haspamelodica.javazmach.assembler.core.Diagnostic.Severity.WARNING;

import net.haspamelodica.javazmach.assembler.core.Diagnostic.Severity;

@FunctionalInterface
public interface DiagnosticHandler
{
	public final static DiagnosticHandler DEFAULT_HANDLER = (diagnostic) -> defaultEmit(diagnostic);

	public void emit(Diagnostic diagnostic);

	public default void emit(Severity severity, String message)
	{
		emit(new Diagnostic(severity, message));
	}
	public default void error(String message)
	{
		emit(ERROR, message);
	}
	public default void warning(String message)
	{
		emit(WARNING, message);
	}
	public default void info(String message)
	{
		emit(INFO, message);
	}

	public static void defaultEmit(Diagnostic diagnostic)
	{
		switch(diagnostic.severity())
		{
			case ERROR -> defaultError(diagnostic.message());
			case WARNING -> defaultWarning(diagnostic.message());
			case INFO -> defaultInfo(diagnostic.message());
		}
	}
	public static <R> R defaultError(String message)
	{
		throw new IllegalArgumentException(message);
	}
	public static void defaultWarning(String message)
	{
		System.err.println("WARNING: " + message);
	}
	public static void defaultInfo(String message)
	{
		System.err.println("INFO: " + message);
	}

	public static DiagnosticHandler defaultHandler()
	{
		return DiagnosticHandler::defaultEmit;
	}
}
