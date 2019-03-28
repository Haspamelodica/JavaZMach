package net.haspamelodica.javaz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.model.ZInterpreter;
import net.haspamelodica.javaz.model.memory.WritableFixedSizeMemory;

public class JavaZ
{
	public static void main(String[] args) throws IOException
	{
		//TODO read config and game path from commandline / args
		WritableFixedSizeMemory mem = new WritableFixedSizeMemory(Files.readAllBytes(Paths.get("zork1.z3")));
		ZInterpreter zInterpreter = new ZInterpreter(new GlobalConfig(), mem, mem);
		zInterpreter.reset();
		while(zInterpreter.step());
	}
}