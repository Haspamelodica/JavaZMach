package net.haspamelodica.javazmach.assembler.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.parser.parser.ParseException;
import net.haspamelodica.parser.tokenizer.CharReader;
import net.haspamelodica.parser.tokenizer.Tokenizer;
import net.haspamelodica.parser.tokenizer.regexbased.RegexBasedTokenizerParser;

public class ZAssemblerTokenizer
{
	public static Tokenizer<CharReader> createTokenizer()
	{
		String tokenizerStringRaw;
		try(InputStream resource = ZAssemblerTokenizer.class.getResourceAsStream("tokenizer.txt"))
		{
			if(resource == null)
				throw new RuntimeException("Tokenizer definition not found");
			tokenizerStringRaw = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
		} catch(IOException e)
		{
			throw new RuntimeException("Error reading tokenizer definition", e);
		}

		// Just putting this in one huge regex is too much (causes StackOverflowErrors during tokenizer parsing).
		// So, split in three regexes: original caseness, lowercase, uppercase.
		String opcodesOrigRegex = Arrays.stream(Opcode.values())
				.filter(o -> o != Opcode._unknown_instr).map(o -> o.name)
				.distinct()
				.collect(Collectors.joining("|"));

		String tokenizerString = tokenizerStringRaw
				.replace("OPCODESORIG", opcodesOrigRegex)
				.replace("OPCODESLOWER", opcodesOrigRegex.toLowerCase())
				.replace("OPCODESUPPER", opcodesOrigRegex.toUpperCase());

		try
		{
			return RegexBasedTokenizerParser.create(tokenizerString);
		} catch(ParseException e)
		{
			throw new IllegalArgumentException("Error parsing tokenizer", e);
		}
	}

	private ZAssemblerTokenizer()
	{}
}
