package net.haspamelodica.javazmach.assembler.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import net.haspamelodica.javazmach.assembler.model.ExplicitSection;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.entries.Buffer;
import net.haspamelodica.javazmach.assembler.model.entries.Dictionary;
import net.haspamelodica.javazmach.assembler.model.entries.GlobalVarTable;
import net.haspamelodica.javazmach.assembler.model.entries.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.entries.IdentifierDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.LabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.MacroDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.MacroEntry;
import net.haspamelodica.javazmach.assembler.model.entries.MacroParamIdentifierDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.MacroReference;
import net.haspamelodica.javazmach.assembler.model.entries.NamedValue;
import net.haspamelodica.javazmach.assembler.model.entries.RegularIdentifierDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.Routine;
import net.haspamelodica.javazmach.assembler.model.entries.SectionDeclaration;
import net.haspamelodica.javazmach.assembler.model.entries.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.entries.ZAssemblerInstruction;
import net.haspamelodica.javazmach.assembler.model.entries.ZObjectTable;
import net.haspamelodica.javazmach.assembler.model.entries.dictionary.DictionaryDataElement;
import net.haspamelodica.javazmach.assembler.model.entries.dictionary.DictionaryEntry;
import net.haspamelodica.javazmach.assembler.model.entries.globaltable.Global;
import net.haspamelodica.javazmach.assembler.model.entries.instruction.BranchInfo;
import net.haspamelodica.javazmach.assembler.model.entries.instruction.BranchLength;
import net.haspamelodica.javazmach.assembler.model.entries.macro.MacroParamDecl;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.Property;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.ZAttribute;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.ZObject;
import net.haspamelodica.javazmach.assembler.model.entries.objecttable.ZObjectEntry;
import net.haspamelodica.javazmach.assembler.model.entries.routine.RoutineLocal;
import net.haspamelodica.javazmach.assembler.model.values.BinaryExpression;
import net.haspamelodica.javazmach.assembler.model.values.BranchTarget;
import net.haspamelodica.javazmach.assembler.model.values.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.values.ByteSequenceElement;
import net.haspamelodica.javazmach.assembler.model.values.CString;
import net.haspamelodica.javazmach.assembler.model.values.CharLiteral;
import net.haspamelodica.javazmach.assembler.model.values.GlobalVariable;
import net.haspamelodica.javazmach.assembler.model.values.HeaderValue;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;
import net.haspamelodica.javazmach.assembler.model.values.LabelReference;
import net.haspamelodica.javazmach.assembler.model.values.LabelReferenceIntegralOnly;
import net.haspamelodica.javazmach.assembler.model.values.LabelReferenceMacroArgument;
import net.haspamelodica.javazmach.assembler.model.values.LabelReferenceVariableOnly;
import net.haspamelodica.javazmach.assembler.model.values.LocalVariable;
import net.haspamelodica.javazmach.assembler.model.values.MacroArgument;
import net.haspamelodica.javazmach.assembler.model.values.MacroParam;
import net.haspamelodica.javazmach.assembler.model.values.MacroParamRef;
import net.haspamelodica.javazmach.assembler.model.values.NumberLiteral;
import net.haspamelodica.javazmach.assembler.model.values.Operand;
import net.haspamelodica.javazmach.assembler.model.values.SimpleBranchTarget;
import net.haspamelodica.javazmach.assembler.model.values.StackPointer;
import net.haspamelodica.javazmach.assembler.model.values.StoreTarget;
import net.haspamelodica.javazmach.assembler.model.values.UnaryExpression;
import net.haspamelodica.javazmach.assembler.model.values.ZString;
import net.haspamelodica.javazmach.assembler.model.values.zstrings.ZStringElement;
import net.haspamelodica.javazmach.core.instructions.OpcodeForm;
import net.haspamelodica.parser.caching.DigestingInputStream;
import net.haspamelodica.parser.caching.LRkParserSerializer;
import net.haspamelodica.parser.caching.LRkParserSerializer.Symbols;
import net.haspamelodica.parser.caching.VersionMagicMismatchException;
import net.haspamelodica.parser.generics.ParameterizedTypeImpl;
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
import net.haspamelodica.parser.tokenizer.CharString;

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
		System.out.println("Regenerating grammar cache into " + out + ":");
		System.out.println("\tParsing grammar...");
		ParsedGrammar parsedGrammar = parseGrammar(ZAssemblerTokenizer.createTokenizer().allTerminals());
		System.out.println("\tGenerating parser...");
		LRkParser parser = generateUncachedParser(parsedGrammar.grammarResult());
		System.out.println("\tWriting cache...");
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
		return LRkParserGenerator.generate(grammarResult.getGrammar(), 2);
	}

	public static ParsedGrammar parseGrammar(Set<Terminal<?>> allTerminals)
	{
		Map<String, TypedFunction> functionsByName = createFunctionsByName();

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

	private static Map<String, TypedFunction> createFunctionsByName()
	{
		ParameterizedType T_ListZAssemblyFileEntry = new ParameterizedTypeImpl(null, List.class, ZAssemblerFileEntry.class);
		ParameterizedType T_ListOperand = new ParameterizedTypeImpl(null, List.class, Operand.class);
		ParameterizedType T_ListByteSequenceElement = new ParameterizedTypeImpl(null, List.class, ByteSequenceElement.class);
		ParameterizedType T_ListZStringElement = new ParameterizedTypeImpl(null, List.class, ZStringElement.class);
		ParameterizedType T_ListRoutineLocal = new ParameterizedTypeImpl(null, List.class, RoutineLocal.class);
		ParameterizedType T_ListGlobal = new ParameterizedTypeImpl(null, List.class, Global.class);
		ParameterizedType T_ListProperty = new ParameterizedTypeImpl(null, List.class, Property.class);
		ParameterizedType T_ListObject = new ParameterizedTypeImpl(null, List.class, ZObject.class);
		ParameterizedType T_ListObjectEntry = new ParameterizedTypeImpl(null, List.class, ZObjectEntry.class);
		ParameterizedType T_ListDictionaryEntry = new ParameterizedTypeImpl(null, List.class, DictionaryEntry.class);
		ParameterizedType T_ListDataEntry = new ParameterizedTypeImpl(null, List.class, DictionaryDataElement.class);
		ParameterizedType T_ListChar = new ParameterizedTypeImpl(null, List.class, CharLiteral.class);
		ParameterizedType T_ListMacroEntry = new ParameterizedTypeImpl(null, List.class, MacroEntry.class);
		ParameterizedType T_ListMacroParamDecl = new ParameterizedTypeImpl(null, List.class, MacroParamDecl.class);
		ParameterizedType T_ListMacroArg = new ParameterizedTypeImpl(null, List.class, MacroArgument.class);
		ParameterizedType T_OptForm = new ParameterizedTypeImpl(null, Optional.class, OpcodeForm.class);
		ParameterizedType T_OptStoreTarget = new ParameterizedTypeImpl(null, Optional.class, StoreTarget.class);
		ParameterizedType T_OptIntegralValue = new ParameterizedTypeImpl(null, Optional.class, IntegralValue.class);
		ParameterizedType T_OptBranchInfo = new ParameterizedTypeImpl(null, Optional.class, BranchInfo.class);
		ParameterizedType T_OptBLO = new ParameterizedTypeImpl(null, Optional.class, BranchLength.class);
		ParameterizedType T_OptZString = new ParameterizedTypeImpl(null, Optional.class, ZString.class);
		ParameterizedType T_OptByteSequence = new ParameterizedTypeImpl(null, Optional.class, ByteSequence.class);
		ParameterizedType T_OptString = new ParameterizedTypeImpl(null, Optional.class, String.class);

		Map<String, TypedFunction> functionsByName = new HashMap<>();

		// constructors
		functionsByName.put("ZAssemblerFile", TypedFunction.buildT(ZAssemblerFile::new,
				ZAssemblerFile.class, OptionalInt.class, T_ListZAssemblyFileEntry));
		functionsByName.put("HeaderEntry", TypedFunction.build(HeaderEntry::new, HeaderEntry.class, String.class, HeaderValue.class));
		functionsByName.put("GlobalVarTable", TypedFunction.buildT(GlobalVarTable::new, GlobalVarTable.class, T_ListGlobal));
		functionsByName.put("Global", TypedFunction.buildT(Global::new, Global.class, String.class, T_OptIntegralValue));
		functionsByName.put("Routine", TypedFunction.buildT(Routine::new, Routine.class, IdentifierDeclaration.class, T_ListRoutineLocal));
		functionsByName.put("RoutineLocal", TypedFunction.buildT(RoutineLocal::new, RoutineLocal.class, IdentifierDeclaration.class, T_OptIntegralValue));
		functionsByName.put("Property", TypedFunction.build(Property::new, Property.class, BigInteger.class, ByteSequence.class));
		functionsByName.put("ZObjectTable", TypedFunction.buildT(ZObjectTable::new, ZObjectTable.class, T_ListProperty, T_ListObject));
		functionsByName.put("ZObject", TypedFunction.buildT(ZObject::new, ZObject.class, T_OptString, ZString.class, T_ListObjectEntry));
		functionsByName.put("Dictionary", TypedFunction.buildT(Dictionary::new, Dictionary.class, T_ListChar, T_ListDictionaryEntry));
		functionsByName.put("DictionaryEntry", TypedFunction.buildT(DictionaryEntry::new, DictionaryEntry.class, ZString.class, T_ListDataEntry));
		functionsByName.put("DictionaryDataElement", TypedFunction.build(DictionaryDataElement::new,
				DictionaryDataElement.class, BigInteger.class, HeaderValue.class));
		functionsByName.put("SectionDeclaration", TypedFunction.buildT(SectionDeclaration::new,
				SectionDeclaration.class, ExplicitSection.class, T_OptIntegralValue));
		functionsByName.put("ZAttribute", TypedFunction.build(ZAttribute::new, ZAttribute.class, BigInteger.class));
		functionsByName.put("LabelDeclaration", TypedFunction.build(LabelDeclaration::new, LabelDeclaration.class, IdentifierDeclaration.class));
		functionsByName.put("Instruction", TypedFunction.buildT(ZAssemblerInstruction::new,
				ZAssemblerInstruction.class, String.class, T_OptForm, T_ListOperand, T_OptStoreTarget, T_OptBranchInfo, T_OptZString));
		functionsByName.put("BranchInfo", TypedFunction.buildT(BranchInfo::new,
				BranchInfo.class, Boolean.class, BranchTarget.class, T_OptBLO));
		functionsByName.put("LabelReference", TypedFunction.build(LabelReference::new, LabelReference.class, String.class));
		functionsByName.put("LabelReferenceIntegralOnly", TypedFunction.build(LabelReferenceIntegralOnly::new,
				LabelReferenceIntegralOnly.class, String.class));
		functionsByName.put("LabelReferenceVariableOnly", TypedFunction.build(LabelReferenceVariableOnly::new,
				LabelReferenceVariableOnly.class, String.class));
		functionsByName.put("LocalVariable", TypedFunction.build(LocalVariable::new, LocalVariable.class, Integer.class));
		functionsByName.put("GlobalVariable", TypedFunction.build(GlobalVariable::new, GlobalVariable.class, Integer.class));
		functionsByName.put("Buffer", TypedFunction.buildT(Buffer::new,
				Buffer.class, IdentifierDeclaration.class, T_OptIntegralValue, T_OptByteSequence));
		functionsByName.put("NamedValue", TypedFunction.build(NamedValue::new, NamedValue.class, IdentifierDeclaration.class, IntegralValue.class));
		functionsByName.put("MacroDeclaration", TypedFunction.buildT(MacroDeclaration::new,
				MacroDeclaration.class, String.class, T_ListMacroParamDecl, T_ListMacroEntry));
		functionsByName.put("MacroParamDecl", TypedFunction.build(MacroParamDecl::new, MacroParamDecl.class, String.class));
		functionsByName.put("MacroReference", TypedFunction.buildT(MacroReference::new, MacroReference.class, String.class, T_ListOperand));
		functionsByName.put("MacroParam", TypedFunction.build(MacroParam::new, MacroParam.class, String.class));
		functionsByName.put("MacroParamRef", TypedFunction.build(MacroParamRef::new, MacroParamRef.class, MacroParam.class));
		functionsByName.put("LabelReferenceMacroArgument", TypedFunction.build(LabelReferenceMacroArgument::new,
				LabelReferenceMacroArgument.class, String.class));
		functionsByName.put("ZString", TypedFunction.buildT(ZString::new, ZString.class, T_ListZStringElement));
		functionsByName.put("ZStringElement", TypedFunction.build(ZStringElement::new, ZStringElement.class, String.class));
		functionsByName.put("CString", TypedFunction.buildT(CString::new, CString.class, String.class));
		functionsByName.put("ByteSequence", TypedFunction.buildT(ByteSequence::new,
				ByteSequence.class, T_ListByteSequenceElement));
		functionsByName.put("BinaryExpression", TypedFunction.build(BinaryExpression::new,
				BinaryExpression.class, IntegralValue.class, BinaryExpression.Op.class, IntegralValue.class));
		functionsByName.put("UnaryExpression", TypedFunction.build(UnaryExpression::new,
				UnaryExpression.class, UnaryExpression.Op.class, IntegralValue.class));
		functionsByName.put("RegularIdentifierDeclaration", TypedFunction.build(RegularIdentifierDeclaration::new,
				RegularIdentifierDeclaration.class, String.class));
		functionsByName.put("MacroParamIdentifierDeclaration", TypedFunction.build(MacroParamIdentifierDeclaration::new,
				MacroParamIdentifierDeclaration.class, MacroParam.class));
		functionsByName.put("NumberLiteral", TypedFunction.build(NumberLiteral::new, NumberLiteral.class, BigInteger.class));
		functionsByName.put("CharLiteral", TypedFunction.build(CharLiteral::new, CharLiteral.class, Character.class));
		functionsByName.put("StringLiteral", TypedFunction.build(CString::new, CString.class, String.class));

		// enum constants
		functionsByName.put("SHORTBRANCH", TypedFunction.build(() -> BranchLength.SHORTBRANCH, BranchLength.class));
		functionsByName.put("LONGBRANCH", TypedFunction.build(() -> BranchLength.LONGBRANCH, BranchLength.class));
		functionsByName.put("rfalse", TypedFunction.build(() -> SimpleBranchTarget.rfalse, SimpleBranchTarget.class));
		functionsByName.put("rtrue", TypedFunction.build(() -> SimpleBranchTarget.rtrue, SimpleBranchTarget.class));
		functionsByName.put("StackPointer", TypedFunction.build(() -> StackPointer.INSTANCE, StackPointer.class));
		functionsByName.put("LONG", TypedFunction.build(() -> OpcodeForm.LONG, OpcodeForm.class));
		functionsByName.put("SHORT", TypedFunction.build(() -> OpcodeForm.SHORT, OpcodeForm.class));
		functionsByName.put("EXTENDED", TypedFunction.build(() -> OpcodeForm.EXTENDED, OpcodeForm.class));
		functionsByName.put("VARIABLE", TypedFunction.build(() -> OpcodeForm.VARIABLE, OpcodeForm.class));
		functionsByName.put("STATIC", TypedFunction.build(() -> ExplicitSection.STATIC, ExplicitSection.class));
		functionsByName.put("HIGH", TypedFunction.build(() -> ExplicitSection.HIGH, ExplicitSection.class));
		functionsByName.put("BITWISE_OR", TypedFunction.build(() -> BinaryExpression.Op.BITWISE_OR, BinaryExpression.Op.class));
		functionsByName.put("BITWISE_XOR", TypedFunction.build(() -> BinaryExpression.Op.BITWISE_XOR, BinaryExpression.Op.class));
		functionsByName.put("BITWISE_AND", TypedFunction.build(() -> BinaryExpression.Op.BITWISE_AND, BinaryExpression.Op.class));
		functionsByName.put("LSHIFT", TypedFunction.build(() -> BinaryExpression.Op.LSHIFT, BinaryExpression.Op.class));
		functionsByName.put("RSHIFT", TypedFunction.build(() -> BinaryExpression.Op.RSHIFT, BinaryExpression.Op.class));
		functionsByName.put("ADD", TypedFunction.build(() -> BinaryExpression.Op.ADD, BinaryExpression.Op.class));
		functionsByName.put("SUBTRACT", TypedFunction.build(() -> BinaryExpression.Op.SUBTRACT, BinaryExpression.Op.class));
		functionsByName.put("MULTIPLY", TypedFunction.build(() -> BinaryExpression.Op.MULTIPLY, BinaryExpression.Op.class));
		functionsByName.put("DIVIDE", TypedFunction.build(() -> BinaryExpression.Op.DIVIDE, BinaryExpression.Op.class));
		functionsByName.put("MODULO", TypedFunction.build(() -> BinaryExpression.Op.MODULO, BinaryExpression.Op.class));
		functionsByName.put("NEGATE", TypedFunction.build(() -> UnaryExpression.Op.NEGATE, UnaryExpression.Op.class));
		functionsByName.put("BITWISE_NOT", TypedFunction.build(() -> UnaryExpression.Op.BITWISE_NOT, UnaryExpression.Op.class));

		// lists
		functionsByName.put("emptyAEList", TypedFunction.buildT(ArrayList::new, T_ListZAssemblyFileEntry));
		functionsByName.put("appendAEList", TypedFunction.<List<ZAssemblerFileEntry>, ZAssemblerFileEntry, List<ZAssemblerFileEntry>> buildT(
				ZAssemblerParserCache::appendList, T_ListZAssemblyFileEntry, T_ListZAssemblyFileEntry, ZAssemblerFileEntry.class));
		functionsByName.put("emptyRLList", TypedFunction.buildT(ArrayList::new, T_ListRoutineLocal));
		functionsByName.put("appendRLList", TypedFunction.<List<RoutineLocal>, RoutineLocal, List<RoutineLocal>> buildT(
				ZAssemblerParserCache::appendList, T_ListRoutineLocal, T_ListRoutineLocal, RoutineLocal.class));
		functionsByName.put("emptyGlList", TypedFunction.buildT(ArrayList::new, T_ListGlobal));
		functionsByName.put("appendGlList", TypedFunction.<List<Global>, Global, List<Global>> buildT(
				ZAssemblerParserCache::appendList, T_ListGlobal, T_ListGlobal, Global.class));
		functionsByName.put("emptyPropList", TypedFunction.buildT(ArrayList::new, T_ListProperty));
		functionsByName.put("appendPropList", TypedFunction.<List<Property>, Property, List<Property>> buildT(
				ZAssemblerParserCache::appendList, T_ListProperty, T_ListProperty, Property.class));
		functionsByName.put("emptyObjList", TypedFunction.buildT(ArrayList::new, T_ListObject));
		functionsByName.put("appendObjList", TypedFunction.<List<ZObject>, ZObject, List<ZObject>> buildT(
				ZAssemblerParserCache::appendList, T_ListObject, T_ListObject, ZObject.class));
		functionsByName.put("emptyOEList", TypedFunction.buildT(ArrayList::new, T_ListObjectEntry));
		functionsByName.put("appendOEList", TypedFunction.<List<ZObjectEntry>, ZObjectEntry, List<ZObjectEntry>> buildT(
				ZAssemblerParserCache::appendList, T_ListObjectEntry, T_ListObjectEntry, ZObjectEntry.class));
		functionsByName.put("emptyDEList", TypedFunction.buildT(ArrayList::new, T_ListDictionaryEntry));
		functionsByName.put("appendDEList", TypedFunction.<List<DictionaryEntry>, DictionaryEntry, List<DictionaryEntry>> buildT(
				ZAssemblerParserCache::appendList, T_ListDictionaryEntry, T_ListDictionaryEntry, DictionaryEntry.class));
		functionsByName.put("emptyDDEList", TypedFunction.buildT(ArrayList::new, T_ListDataEntry));
		functionsByName.put("appendDDEList", TypedFunction.<List<DictionaryDataElement>, DictionaryDataElement, List<DictionaryDataElement>> buildT(
				ZAssemblerParserCache::appendList, T_ListDataEntry, T_ListDataEntry, DictionaryDataElement.class));
		functionsByName.put("emptyOperandList", TypedFunction.buildT(ArrayList::new, T_ListOperand));
		functionsByName.put("appendOperandList", TypedFunction.<List<Operand>, Operand, List<Operand>> buildT(
				ZAssemblerParserCache::appendList, T_ListOperand, T_ListOperand, Operand.class));
		functionsByName.put("emptyByteSequenceList", TypedFunction.buildT(ArrayList::new, T_ListByteSequenceElement));
		functionsByName.put("appendByteSequenceList", TypedFunction.<List<ByteSequenceElement>, ByteSequenceElement, List<ByteSequenceElement>> buildT(
				ZAssemblerParserCache::appendList, T_ListByteSequenceElement, T_ListByteSequenceElement, ByteSequenceElement.class));
		functionsByName.put("emptyZStringElementList", TypedFunction.buildT(ArrayList::new, T_ListZStringElement));
		functionsByName.put("appendZStringElementList", TypedFunction.<List<ZStringElement>, ZStringElement, List<ZStringElement>> buildT(
				ZAssemblerParserCache::appendList, T_ListZStringElement, T_ListZStringElement, ZStringElement.class));
		functionsByName.put("emptyCList", TypedFunction.buildT(ArrayList::new, T_ListChar));
		functionsByName.put("appendCList", TypedFunction.<List<CharLiteral>, CharLiteral, List<CharLiteral>> buildT(
				ZAssemblerParserCache::appendList, T_ListChar, T_ListChar, CharLiteral.class));
		functionsByName.put("emptyMEList", TypedFunction.buildT(ArrayList::new, T_ListMacroEntry));
		functionsByName.put("appendMEList", TypedFunction.<List<MacroEntry>, MacroEntry, List<MacroEntry>> buildT(
				ZAssemblerParserCache::appendList, T_ListMacroEntry, T_ListMacroEntry, MacroEntry.class));
		functionsByName.put("emptyMPDList", TypedFunction.buildT(ArrayList::new, T_ListMacroParamDecl));
		functionsByName.put("appendMPDList", TypedFunction.<List<MacroParamDecl>, MacroParamDecl, List<MacroParamDecl>> buildT(
				ZAssemblerParserCache::appendList, T_ListMacroParamDecl, T_ListMacroParamDecl, MacroParamDecl.class));
		functionsByName.put("emptyMacroArgList", TypedFunction.buildT(ArrayList::new, T_ListMacroArg));
		functionsByName.put("appendMacroArgList", TypedFunction.<List<MacroArgument>, MacroArgument, List<MacroArgument>> buildT(
				ZAssemblerParserCache::appendList, T_ListMacroArg, T_ListMacroArg, MacroArgument.class));

		// C-Strings
		functionsByName.put("appendCString", TypedFunction.build(CString::append, CString.class, CString.class, String.class));

		// Optionals
		functionsByName.put("optFormEmpty", TypedFunction.buildT(Optional::empty, T_OptForm));
		functionsByName.put("optFormOf", TypedFunction.buildT(Optional::of, T_OptForm, OpcodeForm.class));
		functionsByName.put("optStoreTargetEmpty", TypedFunction.buildT(Optional::empty, T_OptStoreTarget));
		functionsByName.put("optStoreTargetOf", TypedFunction.buildT(Optional::of, T_OptStoreTarget, StoreTarget.class));
		functionsByName.put("optIntegralValueEmpty", TypedFunction.buildT(Optional::empty, T_OptIntegralValue));
		functionsByName.put("optIntegralValueOf", TypedFunction.buildT(Optional::of, T_OptIntegralValue, IntegralValue.class));
		functionsByName.put("optBranchInfoEmpty", TypedFunction.buildT(Optional::empty, T_OptBranchInfo));
		functionsByName.put("optBranchInfoOf", TypedFunction.buildT(Optional::of, T_OptBranchInfo, BranchInfo.class));
		functionsByName.put("optBranchLengthEmpty", TypedFunction.buildT(Optional::empty, T_OptBLO));
		functionsByName.put("optBranchLengthOf", TypedFunction.buildT(Optional::of, T_OptBLO, BranchLength.class));
		functionsByName.put("optZStringEmpty", TypedFunction.buildT(Optional::empty, T_OptZString));
		functionsByName.put("optZStringOf", TypedFunction.buildT(Optional::of, T_OptZString, ZString.class));
		functionsByName.put("optByteSequenceEmpty", TypedFunction.buildT(Optional::empty, T_OptByteSequence));
		functionsByName.put("optByteSequenceOf", TypedFunction.buildT(Optional::of, T_OptByteSequence, ByteSequence.class));
		functionsByName.put("optStringEmpty", TypedFunction.buildT(Optional::empty, T_OptString));
		functionsByName.put("optStringOf", TypedFunction.buildT(Optional::of, T_OptString, String.class));
		functionsByName.put("optIntEmpty", TypedFunction.build(OptionalInt::empty, OptionalInt.class));
		functionsByName.put("optIntOf", TypedFunction.build(OptionalInt::of, OptionalInt.class, Integer.class));

		// Strings, ints, booleans
		functionsByName.put("str", TypedFunction.build(CharString::toStringNoEscaping, String.class, CharString.class));
		functionsByName.put("parseText", TypedFunction.build(ZAssemblerParserCache::parseText, String.class, CharString.class));
		functionsByName.put("parseCText", TypedFunction.build(ZAssemblerParserCache::parseCText, String.class, CharString.class));
		functionsByName.put("parseChar", TypedFunction.build(ZAssemblerParserCache::parseChar, Character.class, CharString.class));
		functionsByName.put("parseBigInt", TypedFunction.build(ZAssemblerParserCache::parseBigInt, BigInteger.class, Integer.class, Integer.class, CharString.class));
		functionsByName.put("int", TypedFunction.build(BigInteger::intValueExact, Integer.class, BigInteger.class));
		functionsByName.put("_0", TypedFunction.build(() -> 0, Integer.class));
		functionsByName.put("_1", TypedFunction.build(() -> 1, Integer.class));
		functionsByName.put("_2", TypedFunction.build(() -> 2, Integer.class));
		functionsByName.put("_3", TypedFunction.build(() -> 3, Integer.class));
		functionsByName.put("_10", TypedFunction.build(() -> 10, Integer.class));
		functionsByName.put("_16", TypedFunction.build(() -> 16, Integer.class));
		functionsByName.put("true", TypedFunction.build(() -> true, Boolean.class));
		functionsByName.put("false", TypedFunction.build(() -> false, Boolean.class));
		return functionsByName;
	}

	private static String parseText(CharString cs)
	{
		String rawText = cs.toStringNoEscaping();
		if(rawText.charAt(0) != '"' || rawText.charAt(rawText.length() - 1) != '"')
			throw new IllegalArgumentException("Invalid text literal: " + cs);

		return parseTextOrChar(rawText, 1, rawText.length() - 1, true, "text");
	}

	private static String parseCText(CharString cs)
	{
		String rawText = cs.toStringNoEscaping();
		if("cC".indexOf(rawText.charAt(0)) >= 0 && rawText.charAt(1) != '"' || rawText.charAt(rawText.length() - 1) != '"')
			throw new IllegalArgumentException("Invalid text literal: " + cs);

		return parseTextOrChar(rawText, 2, rawText.length() - 1, true, "text");
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

	private ZAssemblerParserCache()
	{}
}
