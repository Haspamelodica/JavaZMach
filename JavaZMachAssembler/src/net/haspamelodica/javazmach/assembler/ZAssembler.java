package net.haspamelodica.javazmach.assembler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.parser.parser.ParseException;
import net.haspamelodica.parser.tokenizer.CharReader;

public class ZAssembler
{
	private final ZAssemblerFile		file;
	private final int					version;
	private final Map<String, Opcode>	opcodesByNameLowercase;

	public ZAssembler(ZAssemblerFile file, int versionFromCommandline)
	{
		// TODO the version error messages break separation of concerns:
		// this constructor doesn't know that the version arg comes from the commandline,
		// let alone how the specific switch is called.
		if(versionFromCommandline <= 0)
			this.version = file.version().orElseThrow(() -> new IllegalArgumentException(
					"Z-version not given: neither by commandline -z / --zversion, nor by .ZVERSION in file"));
		else if(file.version().isEmpty())
			this.version = versionFromCommandline;
		else if(file.version().getAsInt() == versionFromCommandline)
			this.version = versionFromCommandline;
		else
			throw new IllegalArgumentException("Z-Version from commandline -z / --zversion mismatches .ZVERSION in file");

		this.file = file;
		this.opcodesByNameLowercase = Arrays
				.stream(Opcode.values())
				.filter(o -> o != Opcode._unknown_instr)
				.filter(o -> version >= o.minVersion)
				.filter(o -> version <= o.maxVersion || o.maxVersion <= 0)
				// careful: don't use method "name()", but member "name".
				.collect(Collectors.toUnmodifiableMap(o -> o.name.toLowerCase(), o -> o));
	}

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
			throw new IllegalArgumentException("Usage: " + ZAssembler.class.getSimpleName()
					+ " [-z <zversion> | --zversion <zversion> ] <input_assembly_file> <output_storyfile>");

		ZAssemblerFile file;
		try(BufferedReader in = Files.newBufferedReader(Path.of(arguments.get(0))))
		{
			file = ZAssemblerParser.parse(CharReader.fromReader(in));
		}

		System.out.println("Version: " + file.version());
		System.out.println("Header:");
		file.headerEntries().forEach(e -> System.out.println("  " + e));
		System.out.println("Instructions:");
		file.instructions().forEach(i -> System.out.println("  " + i));
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
