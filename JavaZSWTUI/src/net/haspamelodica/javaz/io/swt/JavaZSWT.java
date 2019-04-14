package net.haspamelodica.javaz.io.swt;

import static net.haspamelodica.javaz.JavaZRunner.createInterpreter;
import static net.haspamelodica.javaz.JavaZRunner.readConfigFromArgs;

import java.io.IOException;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.ZInterpreter;

public class JavaZSWT
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = readConfigFromArgs(args);
		ZInterpreter zInterpreter = createInterpreter(config, new SWTVideoCard(config));
		zInterpreter.reset();
		while(zInterpreter.step());
	}
}