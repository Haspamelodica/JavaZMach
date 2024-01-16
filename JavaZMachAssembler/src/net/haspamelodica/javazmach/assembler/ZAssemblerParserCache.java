package net.haspamelodica.javazmach.assembler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.parser.caching.DigestingInputStream;
import net.haspamelodica.parser.caching.LRkParserSerializer;
import net.haspamelodica.parser.caching.LRkParserSerializer.Symbols;
import net.haspamelodica.parser.caching.VersionMagicMismatchException;
import net.haspamelodica.parser.generics.TypedFunction;
import net.haspamelodica.parser.grammar.ContextFreeGrammar;
import net.haspamelodica.parser.grammar.Nonterminal;
import net.haspamelodica.parser.grammar.Terminal;
import net.haspamelodica.parser.grammar.attributes.Attribute;
import net.haspamelodica.parser.grammar.attributes.evaluating.lattributed.LAttributedEvaluator;
import net.haspamelodica.parser.grammar.parser.AttributeGrammarParseResult;
import net.haspamelodica.parser.grammar.parser.GrammarParser;
import net.haspamelodica.parser.parser.ParseException;
import net.haspamelodica.parser.parser.Parser;
import net.haspamelodica.parser.parser.lrk.GenericLRkParser;
import net.haspamelodica.parser.parser.lrk.LRkParser;
import net.haspamelodica.parser.parser.lrk.LRkParserGenerator;
import net.haspamelodica.parser.tokenizer.CharReader;

public class ZAssemblerParserCache
{
	private static final String	CACHE_RESOURCE_NAME		= "grammar.txt.cache";
	private static final String	GRAMMAR_RESOURCE_NAME	= "grammar.txt";

	public static record ZAssemblerGrammar(Parser parser, LAttributedEvaluator attributeEvaluator, Attribute<ZAssemblerFile> assemblerFileAttr)
	{}

	private static record ParsedGrammar(AttributeGrammarParseResult grammarResult, byte[] grammarDigest)
	{}

	public static void main(String[] args) throws IOException
	{
		Path out = Path.of(args[0]);
		System.out.println("Regenerating " + CACHE_RESOURCE_NAME + " in file " + out + "...");
		ParsedGrammar parsedGrammar = parseGrammar(ZAssemblerTokenizer.createTokenizer().allTerminals());
		LRkParser parser = generateUncachedParser(parsedGrammar.grammarResult());
		serializeParser(parsedGrammar.grammarDigest(), parser, out);
		System.out.println("Done!");
	}

	public static void serializeParser(byte[] checksum, GenericLRkParser<?> parser, Path output) throws IOException
	{
		try(OutputStream out = new DeflaterOutputStream(Files.newOutputStream(output)))
		{
			out.write(checksum);
			LRkParserSerializer.serialize(parser, (t, d) -> d.writeUTF(t.getName()), (n, d) -> d.writeUTF(n.getName()), out);
		}
	}

	public static ZAssemblerGrammar createParser(Set<Terminal<?>> allTerminals)
	{
		ParsedGrammar parsedGrammar = parseGrammar(allTerminals);
		AttributeGrammarParseResult grammarResult = parsedGrammar.grammarResult();
		GenericLRkParser<?> parser = loadCachedParser(parsedGrammar.grammarDigest(), grammarResult.getGrammar());
		if(parser == null)
			parser = generateUncachedParser(grammarResult);

		LAttributedEvaluator attributeEvaluator = new LAttributedEvaluator(parsedGrammar.grammarResult().getAttributeSystem());
		@SuppressWarnings("unchecked")
		Attribute<ZAssemblerFile> programAttrL = (Attribute<ZAssemblerFile>) parsedGrammar.grammarResult().getAttributesByName().get("file");

		return new ZAssemblerGrammar(parser, attributeEvaluator, programAttrL);
	}

	public static LRkParser generateUncachedParser(AttributeGrammarParseResult grammarResult)
	{
		// This grammar is not LR(1) in two places: when discerning ".zversion" and ".zheader" after the dot,
		// and when discerning whether an ident is a label declaration or a label usage as an operand.
		LRkParser originalParser = LRkParserGenerator.generate(grammarResult.getGrammar(), 2);
		return originalParser;
	}

	public static ParsedGrammar parseGrammar(Set<Terminal<?>> allTerminals)
	{
		Map<String, TypedFunction> functionsByName = ZAssemblerParser.createFunctionsByName();

		ParsedGrammar parsedGrammar;
		try(InputStream resource = ZAssemblerParserCache.class.getResourceAsStream(GRAMMAR_RESOURCE_NAME))
		{
			if(resource == null)
				throw new RuntimeException("Grammar definition not found");
			DigestingInputStream digestingIn = new DigestingInputStream(resource, MessageDigest.getInstance("SHA256"));
			AttributeGrammarParseResult grammarResult = GrammarParser.parseAttributeGrammar(CharReader.fromReader(new InputStreamReader(digestingIn)), allTerminals, functionsByName);
			Set<Nonterminal> unreachableNonterminals = grammarResult.getGrammar().calculateUnreachableNonterminals();
			if(!unreachableNonterminals.isEmpty())
				System.err.println("WARNING: Grammar contains unreachable symbols - this is an assembler bug: " + unreachableNonterminals);
			parsedGrammar = new ParsedGrammar(grammarResult, digestingIn.digest());
		} catch(ParseException e)
		{
			throw new IllegalArgumentException("Error parsing grammar", e);
		} catch(IOException e)
		{
			throw new RuntimeException("Error reading grammar definition", e);
		} catch(NoSuchAlgorithmException e)
		{
			throw new RuntimeException("No SHA256", e);
		}
		return parsedGrammar;
	}

	public static GenericLRkParser<?> loadCachedParser(byte[] checksum, ContextFreeGrammar grammar)
	{
		try(InputStream grammarParserCacheIn = ZAssemblerParserCache.class.getResourceAsStream(CACHE_RESOURCE_NAME))
		{
			if(grammarParserCacheIn == null)
			{
				System.err.println("WARNING: No parser cache found - regenerate " + CACHE_RESOURCE_NAME);
				return null;
			}

			InputStream deflatedIn = new InflaterInputStream(grammarParserCacheIn);
			if(!Arrays.equals(deflatedIn.readNBytes(checksum.length), checksum))
			{
				System.err.println("WARNING: Cached parser checksum mismatch - regenerate " + CACHE_RESOURCE_NAME);
				return null;

			}

			Symbols symbols = LRkParserSerializer.categorizeSymbols(grammar);
			Map<String, Nonterminal> nonterminals = symbols.nonterminalsExceptGeneratedStart()
					.stream().collect(Collectors.toMap(Nonterminal::getName, Function.identity()));
			Map<String, Terminal<?>> terminals = symbols.terminals()
					.stream().collect(Collectors.toMap(Terminal::getName, Function.identity()));

			return LRkParserSerializer.deserialize(deflatedIn, d -> terminals.get(d.readUTF()), d -> nonterminals.get(d.readUTF()));
		} catch(IOException e)
		{
			throw new UncheckedIOException(e);
		} catch(VersionMagicMismatchException e)
		{
			System.err.println("WARNING: Cached parser version mismatch - regenerate " + CACHE_RESOURCE_NAME);
			return null;
		}
	}

	private ZAssemblerParserCache()
	{}
}
