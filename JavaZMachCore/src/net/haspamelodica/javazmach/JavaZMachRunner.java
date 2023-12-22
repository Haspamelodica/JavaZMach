package net.haspamelodica.javazmach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javazmach.core.ZInterpreter;
import net.haspamelodica.javazmach.core.io.VideoCard;
import net.haspamelodica.javazmach.core.memory.StaticArrayBackedMemory;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverter;

public class JavaZMachRunner
{
	public static void run(String[] args, VideoCard videoCard, UnicodeZSCIIConverter unicodeZSCIIConverter) throws IOException
	{
		run(readConfigFromArgs(args), videoCard, unicodeZSCIIConverter);
	}
	public static GlobalConfig readConfigFromArgs(String[] args)
	{
		GlobalConfig config = new GlobalConfig();
		config.setBool("interpreter.debug.logs.instructions", false);
		String storyfilePath = null;
		for(String arg : args)
		{
			int indexOfEquals = arg.indexOf('=');
			if(indexOfEquals < 0)
				if(storyfilePath != null)
					throw new IllegalArgumentException("More than one storyfile given: " + storyfilePath + " and " + arg);
				else
					storyfilePath = arg;
			else
			{
				String key = arg.substring(0, indexOfEquals);
				String value = arg.substring(indexOfEquals + 1);
				if(key.equals("storyfile_path"))
					storyfilePath = value;
				else
					config.setString(key, value);
			}
		}
		if(storyfilePath != null)
			config.setString("storyfile_path", storyfilePath);
		return config;
	}
	public static void run(GlobalConfig config, VideoCard videoCard, UnicodeZSCIIConverter unicodeZSCIIConverter) throws IOException
	{
		ZInterpreter zInterpreter = createInterpreter(config, videoCard, unicodeZSCIIConverter);
		zInterpreter.reset();
		while(zInterpreter.step());
	}
	public static ZInterpreter createInterpreter(GlobalConfig config, VideoCard videoCard, UnicodeZSCIIConverter unicodeZSCIIConverter) throws IOException
	{
		return new ZInterpreter(config, readStoryfileROM(config), videoCard, unicodeZSCIIConverter);
	}
	public static StaticArrayBackedMemory readStoryfileROM(GlobalConfig config) throws IOException
	{
		return new StaticArrayBackedMemory(Files.readAllBytes(Paths.get(config.getString("storyfile_path"))));
	}
}
