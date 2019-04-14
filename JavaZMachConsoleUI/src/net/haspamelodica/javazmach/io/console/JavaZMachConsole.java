package net.haspamelodica.javazmach.io.console;

import static net.haspamelodica.javazmach.JavaZMachRunner.readConfigFromArgs;
import static net.haspamelodica.javazmach.JavaZMachRunner.run;

import java.io.IOException;

import net.haspamelodica.javazmach.GlobalConfig;

public class JavaZMachConsole
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = readConfigFromArgs(args);
		run(config, new ConsoleVideoCard(config));
	}
}