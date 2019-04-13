package net.haspamelodica.javaz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.core.ZInterpreter;
import net.haspamelodica.javaz.core.io.VideoCard;
import net.haspamelodica.javaz.core.memory.StaticArrayBackedMemory;

public class JavaZRunner
{
	public static void run(String[] args, VideoCard videoCard) throws IOException
	{
		run(readConfigFromArgs(args), videoCard);
	}
	public static GlobalConfig readConfigFromArgs(String[] args)
	{
		//TODO read config and game path from commandline / args
		GlobalConfig config = new GlobalConfig();
		config.setBool("interpreter.debug.logs.instructions", false);
		//String storyfilePath = "../storyfiles/tests/czech_0_8/czech.z5";
		//String storyfilePath = "../storyfiles/tests/etude/etude.z5";
		String storyfilePath = "../storyfiles/zork1.z3";
		//String storyfilePath = "../storyfiles/trinity.z4";
		//String storyfilePath = "../storyfiles/sanddancer.z8";
		config.setString("storyfile_path", storyfilePath);
		return config;
	}
	public static void run(GlobalConfig config, VideoCard videoCard) throws IOException
	{
		ZInterpreter zInterpreter = createInterpreter(config, videoCard);
		zInterpreter.reset();
		while(zInterpreter.step());
	}
	public static ZInterpreter createInterpreter(GlobalConfig config, VideoCard videoCard) throws IOException
	{
		ZInterpreter zInterpreter = new ZInterpreter(config, readStoryfileROM(config), videoCard);
		return zInterpreter;
	}
	public static StaticArrayBackedMemory readStoryfileROM(GlobalConfig config) throws IOException
	{
		return new StaticArrayBackedMemory(Files.readAllBytes(Paths.get(config.getString("storyfile_path"))));
	}
}