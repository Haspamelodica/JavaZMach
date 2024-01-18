package net.haspamelodica.javazmach.assembler;

import static net.haspamelodica.javazmach.assembler.ZAssemblerArgsParser.parseArgs;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.haspamelodica.javazmach.assembler.ZAssemblerArgsParser.Args;
import net.haspamelodica.javazmach.assembler.core.ZAssembler;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.parser.ZAssemblerParser;
import net.haspamelodica.parser.parser.ParseException;
import net.haspamelodica.parser.tokenizer.CharReader;

public class ZAssemblerRunner
{
	public static void main(String[] args) throws IOException, ParseException
	{
		Args parsedArgs = parseArgs(ZAssemblerRunner.class.getSimpleName(), args);

		// Prints the filename of the input / output file - useful for using inside Docker.
		if(System.getenv("ZASM_ONLY_PRINT_FILENAME_IN") != null)
		{
			System.out.println(parsedArgs.inputAssemblyFile());
			return;
		}
		if(System.getenv("ZASM_ONLY_PRINT_FILENAME_OUT") != null)
		{
			System.out.println(parsedArgs.outputStoryfile());
			return;
		}

		// Allow overriding of filenames via environment - useful for using inside Docker.
		String inputAssemblyFileOverride = System.getenv("ZASM_ARGS_OVERRIDE_FILENAME_IN");
		String inputAssemblyFile = inputAssemblyFileOverride == null ? parsedArgs.inputAssemblyFile() : inputAssemblyFileOverride;

		String outputStoryfileOverride = System.getenv("ZASM_ARGS_OVERRIDE_FILENAME_OUT");
		String outputStoryfile = outputStoryfileOverride == null ? parsedArgs.outputStoryfile() : outputStoryfileOverride;

		ZAssemblerFile file;
		try(BufferedReader in = Files.newBufferedReader(Path.of(inputAssemblyFile)))
		{
			file = ZAssemblerParser.parse(CharReader.fromReader(in));
		}

		byte[] storyfile = ZAssembler.assemble(file, parsedArgs.zversion(), "commandline -z / --zversion");
		Files.write(Path.of(outputStoryfile), storyfile);
	}
}
