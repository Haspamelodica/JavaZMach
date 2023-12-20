package net.haspamelodica.javazmach.assembler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.BranchInfo;
import net.haspamelodica.javazmach.assembler.model.BranchTarget;
import net.haspamelodica.javazmach.assembler.model.ConstantByteSequence;
import net.haspamelodica.javazmach.assembler.model.ConstantByteSequenceElement;
import net.haspamelodica.javazmach.assembler.model.ConstantChar;
import net.haspamelodica.javazmach.assembler.model.ConstantInteger;
import net.haspamelodica.javazmach.assembler.model.ConstantString;
import net.haspamelodica.javazmach.assembler.model.GlobalVariable;
import net.haspamelodica.javazmach.assembler.model.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.HeaderValue;
import net.haspamelodica.javazmach.assembler.model.Label;
import net.haspamelodica.javazmach.assembler.model.LabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.LocalVariable;
import net.haspamelodica.javazmach.assembler.model.Operand;
import net.haspamelodica.javazmach.assembler.model.RFalse;
import net.haspamelodica.javazmach.assembler.model.RTrue;
import net.haspamelodica.javazmach.assembler.model.StackPointer;
import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.instructions.OpcodeForm;
import net.haspamelodica.parser.ast.InnerNode;
import net.haspamelodica.parser.generics.ParameterizedTypeImpl;
import net.haspamelodica.parser.generics.TypedFunction;
import net.haspamelodica.parser.grammar.Nonterminal;
import net.haspamelodica.parser.grammar.attributes.Attribute;
import net.haspamelodica.parser.grammar.attributes.evaluating.lattributed.LAttributedEvaluator;
import net.haspamelodica.parser.grammar.parser.AttributeGrammarParseResult;
import net.haspamelodica.parser.grammar.parser.GrammarParser;
import net.haspamelodica.parser.parser.ParseException;
import net.haspamelodica.parser.parser.Parser;
import net.haspamelodica.parser.parser.lrk.LRkParserGenerator;
import net.haspamelodica.parser.tokenizer.CharReader;
import net.haspamelodica.parser.tokenizer.CharString;
import net.haspamelodica.parser.tokenizer.Tokenizer;
import net.haspamelodica.parser.tokenizer.regexbased.RegexBasedTokenizerParser;

public class ZAssemblerParser
{
	private static final Tokenizer<CharReader>	tokenizer;
	private static final Parser					parser;
	private static final LAttributedEvaluator	attributeEvaluator;

	private static final Attribute<ZAssemblerFile> assemblerFileAttr;

	static
	{
		ParameterizedType T_ListZAssemblyFileEntry = new ParameterizedTypeImpl(null, List.class, ZAssemblerFileEntry.class);
		ParameterizedType T_ListOperand = new ParameterizedTypeImpl(null, List.class, Operand.class);
		ParameterizedType T_ListByteSequenceElement = new ParameterizedTypeImpl(null, List.class, ConstantByteSequenceElement.class);
		ParameterizedType T_OptForm = new ParameterizedTypeImpl(null, Optional.class, OpcodeForm.class);
		ParameterizedType T_OptVariable = new ParameterizedTypeImpl(null, Optional.class, OpcodeForm.class);
		ParameterizedType T_OptBranchInfo = new ParameterizedTypeImpl(null, Optional.class, OpcodeForm.class);
		ParameterizedType T_OptString = new ParameterizedTypeImpl(null, Optional.class, String.class);

		Map<String, TypedFunction> functionsByName = new HashMap<>();

		// "constructors"
		functionsByName.put("ZAssemblerFile", TypedFunction.buildT(ZAssemblerFile::new,
				ZAssemblerFile.class, OptionalInt.class, T_ListZAssemblyFileEntry));
		functionsByName.put("HeaderEntry", TypedFunction.build(HeaderEntry::new, HeaderEntry.class, String.class, HeaderValue.class));
		functionsByName.put("LabelDeclaration", TypedFunction.build(LabelDeclaration::new, LabelDeclaration.class, String.class));
		functionsByName.put("AssemblerZMachInstruction", TypedFunction.buildT(ZAssemblerInstruction::new,
				ZAssemblerInstruction.class, String.class, T_OptForm, T_ListOperand, T_OptVariable, T_OptBranchInfo, T_OptString));
		functionsByName.put("BranchInfo", TypedFunction.build(BranchInfo::new, BranchInfo.class, Boolean.class, BranchTarget.class));
		functionsByName.put("RFalse", TypedFunction.build(() -> RFalse.INSTANCE, RFalse.class));
		functionsByName.put("RTrue", TypedFunction.build(() -> RTrue.INSTANCE, RTrue.class));
		functionsByName.put("Label", TypedFunction.build(Label::new, Label.class, String.class));
		functionsByName.put("StackPointer", TypedFunction.build(() -> StackPointer.INSTANCE, StackPointer.class));
		functionsByName.put("LocalVariable", TypedFunction.build(LocalVariable::new, LocalVariable.class, Integer.class));
		functionsByName.put("GlobalVariable", TypedFunction.build(GlobalVariable::new, GlobalVariable.class, Integer.class));
		functionsByName.put("ConstantByteSequence", TypedFunction.buildT(ConstantByteSequence::new,
				ConstantByteSequence.class, T_ListByteSequenceElement));
		functionsByName.put("ConstantInteger", TypedFunction.build(ConstantInteger::new, ConstantInteger.class, BigInteger.class));
		functionsByName.put("ConstantChar", TypedFunction.build(ConstantChar::new, ConstantChar.class, Character.class));
		functionsByName.put("ConstantString", TypedFunction.build(ConstantString::new, ConstantString.class, String.class));

		// OpcodeForms
		functionsByName.put("LONG", TypedFunction.build(() -> OpcodeForm.LONG, OpcodeForm.class));
		functionsByName.put("SHORT", TypedFunction.build(() -> OpcodeForm.SHORT, OpcodeForm.class));
		functionsByName.put("EXTENDED", TypedFunction.build(() -> OpcodeForm.EXTENDED, OpcodeForm.class));
		functionsByName.put("VARIABLE", TypedFunction.build(() -> OpcodeForm.VARIABLE, OpcodeForm.class));

		// lists
		functionsByName.put("emptyAEList", TypedFunction.buildT(ArrayList::new, T_ListZAssemblyFileEntry));
		functionsByName.put("appendAEList", TypedFunction.buildT(ZAssemblerParser::<ZAssemblerFileEntry> appendList,
				T_ListZAssemblyFileEntry, T_ListZAssemblyFileEntry, ZAssemblerFileEntry.class));
		functionsByName.put("emptyOperandList", TypedFunction.buildT(ArrayList::new, T_ListOperand));
		functionsByName.put("appendOperandList", TypedFunction.buildT(ZAssemblerParser::<Operand> appendList,
				T_ListOperand, T_ListOperand, Operand.class));
		functionsByName.put("emptyByteSequenceList", TypedFunction.buildT(ArrayList::new, T_ListByteSequenceElement));
		functionsByName.put("appendByteSequenceList", TypedFunction.buildT(ZAssemblerParser::<ConstantByteSequenceElement> appendList,
				T_ListByteSequenceElement, T_ListByteSequenceElement, ConstantByteSequenceElement.class));

		// Optionals
		functionsByName.put("optFormEmpty", TypedFunction.buildT(Optional::empty, T_OptForm));
		functionsByName.put("optFormOf", TypedFunction.buildT(Optional::of, T_OptForm, OpcodeForm.class));
		functionsByName.put("optVariableEmpty", TypedFunction.buildT(Optional::empty, T_OptVariable));
		functionsByName.put("optVariableOf", TypedFunction.buildT(Optional::of, T_OptVariable, Variable.class));
		functionsByName.put("optBranchInfoEmpty", TypedFunction.buildT(Optional::empty, T_OptBranchInfo));
		functionsByName.put("optBranchInfoOf", TypedFunction.buildT(Optional::of, T_OptBranchInfo, BranchInfo.class));
		functionsByName.put("optStringEmpty", TypedFunction.buildT(Optional::empty, T_OptString));
		functionsByName.put("optStringOf", TypedFunction.buildT(Optional::of, T_OptString, String.class));
		functionsByName.put("optIntEmpty", TypedFunction.build(OptionalInt::empty, OptionalInt.class));
		functionsByName.put("optIntOf", TypedFunction.build(OptionalInt::of, OptionalInt.class, Integer.class));

		// Strings, ints, booleans
		functionsByName.put("str", TypedFunction.build(CharString::toStringNoEscaping, String.class, CharString.class));
		functionsByName.put("parseText", TypedFunction.build(ZAssemblerParser::parseText, String.class, CharString.class));
		functionsByName.put("parseChar", TypedFunction.build(ZAssemblerParser::parseChar, Character.class, CharString.class));
		functionsByName.put("parseBigInt", TypedFunction.build(ZAssemblerParser::parseBigInt, BigInteger.class, Integer.class, Integer.class, CharString.class));
		functionsByName.put("neg", TypedFunction.build(BigInteger::negate, BigInteger.class, BigInteger.class));
		functionsByName.put("int", TypedFunction.build(BigInteger::intValueExact, Integer.class, BigInteger.class));
		functionsByName.put("byte", TypedFunction.build(BigInteger::byteValueExact, Byte.class, BigInteger.class));
		functionsByName.put("_0", TypedFunction.build(() -> 0, Integer.class));
		functionsByName.put("_1", TypedFunction.build(() -> 1, Integer.class));
		functionsByName.put("_2", TypedFunction.build(() -> 2, Integer.class));
		functionsByName.put("_3", TypedFunction.build(() -> 3, Integer.class));
		functionsByName.put("_10", TypedFunction.build(() -> 10, Integer.class));
		functionsByName.put("_16", TypedFunction.build(() -> 16, Integer.class));
		functionsByName.put("true", TypedFunction.build(() -> true, Boolean.class));
		functionsByName.put("false", TypedFunction.build(() -> false, Boolean.class));

		String tokenizerStringRaw;
		try(InputStream resource = ZAssemblerParser.class.getResourceAsStream("tokenizer.txt"))
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
			tokenizer = RegexBasedTokenizerParser.create(tokenizerString);
		} catch(ParseException e)
		{
			throw new IllegalArgumentException("Error parsing tokenizer", e);
		}

		AttributeGrammarParseResult grammarResult;
		try(InputStream resource = ZAssemblerParser.class.getResourceAsStream("grammar.txt"))
		{
			if(resource == null)
				throw new RuntimeException("Grammar definition not found");
			grammarResult = GrammarParser.parseAttributeGrammar(CharReader.fromReader(new InputStreamReader(resource)), tokenizer.allTerminals(), functionsByName);
		} catch(ParseException e)
		{
			throw new IllegalArgumentException("Error parsing grammar", e);
		} catch(IOException e)
		{
			throw new RuntimeException("Error reading grammar definition", e);
		}
		Set<Nonterminal> unreachableNonterminals = grammarResult.getGrammar().calculateUnreachableNonterminals();
		if(!unreachableNonterminals.isEmpty())
			System.err.println("Warning: grammar contains unreachable symbols: " + unreachableNonterminals);
		// This grammar is not LR(1) in two places: when discerning ".zversion" and ".zheader" after the dot,
		// and when discerning whether an ident is a label declaration or a label usage as an operand.
		parser = LRkParserGenerator.generate(grammarResult.getGrammar(), 2);
		attributeEvaluator = new LAttributedEvaluator(grammarResult.getAttributeSystem());
		@SuppressWarnings("unchecked")
		Attribute<ZAssemblerFile> programAttrL = (Attribute<ZAssemblerFile>) grammarResult.getAttributesByName().get("file");
		assemblerFileAttr = programAttrL;
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

	private static String parseText(CharString cs)
	{
		String rawText = cs.toStringNoEscaping();
		if(rawText.charAt(0) != '"' || rawText.charAt(rawText.length() - 1) != '"')
			throw new IllegalArgumentException("Invalid text literal: " + cs);

		return parseTextOrChar(rawText, 1, rawText.length() - 1, true, "text");
	}
	private static Character parseChar(CharString cs)
	{
		String rawText = cs.toStringNoEscaping();
		if(rawText.charAt(0) != '\'' || rawText.charAt(rawText.length() - 1) != '\'')
			throw new IllegalArgumentException("Invalid char literal: " + cs);

		String parsed = parseTextOrChar(rawText, 1, rawText.length() - 1, false, "char");
		if(parsed.length() != 1)
			throw new IllegalArgumentException("Invalid char literal: " + cs);
		return parsed.charAt(0);
	}

	private static String parseTextOrChar(String rawText, int start, int endExclusive, boolean allowEscapedMultiline, String textOrChar)
	{
		String result = "";
		boolean nextEscaped = false;
		boolean nextIgnoredIfNewline = false;
		for(int i = start; i < endExclusive; i ++)
		{
			char c = rawText.charAt(i);
			if(nextEscaped)
			{
				nextEscaped = false;
				if(!allowEscapedMultiline)
					result += unescape(c);
				else
					switch(c)
					{
						case '\n' -> result += '\n';
						case '\r' ->
						{
							result += '\n';
							nextIgnoredIfNewline = true;
						}
						default -> result += unescape(c);
					}
			} else if(nextIgnoredIfNewline)
			{
				nextIgnoredIfNewline = false;
				if(c == '\\')
					nextEscaped = true;
				else if(c != '\n')
					result += c;
			} else if(c == '\\')
				nextEscaped = true;
			else
				result += c;
		}
		if(nextEscaped)
			throw new IllegalArgumentException("Invalid " + textOrChar + " literal: " + rawText);

		return result;
	}
	private static char unescape(char c)
	{
		return switch(c)
		{
			case '\\' -> '\\';
			case '\'' -> '\'';
			case '"' -> '"';
			case 't' -> '\t';
			case 'r' -> '\r';
			case 'n' -> '\n';
			default -> throw new IllegalArgumentException("Unexpected escaped char: " + c);
		};
	}

	private static BigInteger parseBigInt(int cut, int base, CharString cs)
	{
		String string = cs.toStringNoEscaping();
		try
		{
			return new BigInteger(string.substring(cut), base);
		} catch(NumberFormatException e)
		{
			throw new NumberFormatException("Not a valid integer literal (base " + base + "): " + string);
		}
	}

	private static <E> List<E> appendList(List<E> list, E e)
	{
		list.add(e);
		return list;
	}

	private ZAssemblerParser()
	{}
}
