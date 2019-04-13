package net.haspamelodica.javaz.io.console;

import static net.haspamelodica.javaz.JavaZRunner.readConfigFromArgs;
import static net.haspamelodica.javaz.JavaZRunner.run;

import java.io.IOException;

import net.haspamelodica.javaz.GlobalConfig;

public class JavaZConsole
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = readConfigFromArgs(args);
		run(config, new ConsoleVideoCard(config));
	}
}