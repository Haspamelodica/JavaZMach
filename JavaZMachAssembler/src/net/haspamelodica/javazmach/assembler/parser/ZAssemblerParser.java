package net.haspamelodica.javazmach.assembler.parser;

import java.util.Set;

import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.parser.ZAssemblerParserCache.ZAssemblerGrammar;
import net.haspamelodica.parser.ast.InnerNode;
import net.haspamelodica.parser.grammar.attributes.Attribute;
import net.haspamelodica.parser.grammar.attributes.evaluating.lattributed.LAttributedEvaluator;
import net.haspamelodica.parser.parser.ParseException;
import net.haspamelodica.parser.parser.Parser;
import net.haspamelodica.parser.tokenizer.CharReader;
import net.haspamelodica.parser.tokenizer.Tokenizer;

public class ZAssemblerParser
{
	private static final Tokenizer<CharReader> tokenizer;

	private static final Parser						parser;
	private static final LAttributedEvaluator		attributeEvaluator;
	private static final Attribute<ZAssemblerFile>	assemblerFileAttr;

	static
	{
		tokenizer = ZAssemblerTokenizer.createTokenizer();

		ZAssemblerGrammar grammar = ZAssemblerParserCache.createParser(tokenizer.allTerminals());
		parser = grammar.parser();
		attributeEvaluator = grammar.attributeEvaluator();
		assemblerFileAttr = grammar.assemblerFileAttr();
	}

	public static ZAssemblerFile parse(String in) throws ParseException
	{
		return parse(CharReader.readString(in));
	}
	public static ZAssemblerFile parse(CharReader in) throws ParseException
	{
		InnerNode root = parser.parse(tokenizer.tokenize(in));
		attributeEvaluator.evaluate(root, Set.of());
		return root.getValueForAttribute(assemblerFileAttr);
	}

	private ZAssemblerParser()
	{}
}
