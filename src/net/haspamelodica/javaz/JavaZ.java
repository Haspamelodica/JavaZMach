package net.haspamelodica.javaz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.core.ZInterpreter;
import net.haspamelodica.javaz.core.memory.WritableFixedSizeMemory;
import net.haspamelodica.javaz.io.console.ConsoleVideoCard;

public class JavaZ
{
	public static void main(String[] args) throws IOException
	{
		//TODO read config and game path from commandline / args
		WritableFixedSizeMemory mem = new WritableFixedSizeMemory(Files.readAllBytes(Paths.get("storyfiles/zork1.z3")));
		ZInterpreter zInterpreter = new ZInterpreter(new GlobalConfig(), mem, mem, ConsoleVideoCard::new);
		zInterpreter.reset();
		while(zInterpreter.step());
	}
}