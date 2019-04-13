package net.haspamelodica.javaz.io.console;

import static net.haspamelodica.javaz.JavaZRunner.run;

import java.io.IOException;

public class JavaZConsole
{
	public static void main(String[] args) throws IOException
	{
		run(args, new ConsoleVideoCard());
	}
}