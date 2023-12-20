package net.haspamelodica.javazmach.assembler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.haspamelodica.javazmach.assembler.core.ZAssembler;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.parser.parser.ParseException;
import net.haspamelodica.parser.tokenizer.CharReader;

public class ZAssemblerRunner
{
	public static void main(String[] args) throws IOException, ParseException
	{
		List<String> arguments = new ArrayList<>();
		int zversion = -1;

		for(int i = 0; i < args.length;)
		{
			String arg = args[i ++];
			if(arg.equals("-z") || arg.equals("--zversion"))
			{
				if(i == args.length)
					throw new IllegalArgumentException(arg + ": missing argument");
				zversion = updateZversion(zversion, args[i ++]);
			} else if(arg.startsWith("-z=") || arg.startsWith("--zversion="))
				zversion = updateZversion(zversion, arg.substring(arg.indexOf('=') + 1));
			else if(arg.equals("--"))
				while(i < args.length)
					arguments.add(args[i ++]);
			else
				arguments.add(arg);
		}

		if(arguments.size() != 2)
			throw new IllegalArgumentException("Usage: " + ZAssemblerRunner.class.getSimpleName()
					+ " [-z <zversion> | --zversion <zversion> ] <input_assembly_file> <output_storyfile>");

		ZAssemblerFile file;
		try(BufferedReader in = Files.newBufferedReader(Path.of(arguments.get(0))))
		{
			file = ZAssemblerParser.parse(CharReader.fromReader(in));
		}

		byte[] storyfile = ZAssembler.assemble(file, zversion, "commandline -z / --zversion");
		Files.write(Path.of(arguments.get(1)), storyfile);
	}

	public static int updateZversion(int zversion, String val)
	{
		if(zversion != -1)
			throw new IllegalArgumentException("Duplicate -z / --zversion");

		try
		{
			zversion = Integer.parseInt(val);
		} catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("Not an int: " + val);
		}

		if(zversion <= 0)
			throw new IllegalArgumentException("Illegal zversion: " + zversion);

		return zversion;
	}
}
