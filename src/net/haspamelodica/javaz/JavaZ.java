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
		GlobalConfig config = new GlobalConfig();
		config.setBool("interpreter.debug.logs.instructions", false);
		//String filepath = "storyfiles/tests/czech_0_8/czech.z5";
		//String filepath = "storyfiles/tests/etude/etude.z5";
		String filepath = "storyfiles/zork1.z3";
		//String filepath = "storyfiles/trinity.z4";
		WritableFixedSizeMemory mem = new WritableFixedSizeMemory(Files.readAllBytes(Paths.get(filepath)));
		ZInterpreter zInterpreter = new ZInterpreter(config, mem, mem, ConsoleVideoCard::new);
		zInterpreter.reset();
		while(zInterpreter.step());
	}
}