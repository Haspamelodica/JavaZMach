package net.haspamelodica.javaz.io.swt;

import static net.haspamelodica.javaz.JavaZRunner.createInterpreter;
import static net.haspamelodica.javaz.JavaZRunner.readConfigFromArgs;

import java.io.IOException;

import net.haspamelodica.javaz.core.ZInterpreter;

public class JavaZSWT
{
	public static void main(String[] args) throws IOException
	{
		ZInterpreter zInterpreter = createInterpreter(readConfigFromArgs(args), SWTVideoCard::new);
		zInterpreter.reset();
		while(zInterpreter.step());
	}
}