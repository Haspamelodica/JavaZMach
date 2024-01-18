package net.haspamelodica.javazmach.assembler;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import net.haspamelodica.parser.parser.ParseException;

public class ZAssemblerArgsParser
{
	public static record Args(String inputAssemblyFile, String outputStoryfile, OptionalInt zversion)
	{}

	public static Args parseArgs(String usageMain, String[] args) throws ParseException
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
			throw new IllegalArgumentException("Usage: " + usageMain
					+ " [-z <zversion> | --zversion <zversion> ] <input_assembly_file> <output_storyfile>");

		return new Args(arguments.get(0), arguments.get(1), zversion < 0 ? OptionalInt.empty() : OptionalInt.of(zversion));
	}

	private static int updateZversion(int zversion, String val)
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
