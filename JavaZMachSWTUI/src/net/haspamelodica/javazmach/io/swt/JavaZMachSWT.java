package net.haspamelodica.javazmach.io.swt;

import static net.haspamelodica.javazmach.JavaZMachRunner.createInterpreter;
import static net.haspamelodica.javazmach.JavaZMachRunner.readConfigFromArgs;

import java.io.IOException;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.ZInterpreter;

public class JavaZMachSWT
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = readConfigFromArgs(args);
		ZInterpreter zInterpreter = createInterpreter(config, new SWTVideoCard(config));
		zInterpreter.reset();
		while(zInterpreter.step());
	}
}