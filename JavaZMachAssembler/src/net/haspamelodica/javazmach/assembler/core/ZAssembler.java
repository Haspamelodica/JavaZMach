package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.checkBigintMaxByteCount;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.NoRangeCheckMemory;
import net.haspamelodica.javazmach.assembler.model.ConstantByteSequence;
import net.haspamelodica.javazmach.assembler.model.ConstantByteSequenceElement;
import net.haspamelodica.javazmach.assembler.model.ConstantChar;
import net.haspamelodica.javazmach.assembler.model.ConstantInteger;
import net.haspamelodica.javazmach.assembler.model.ConstantString;
import net.haspamelodica.javazmach.assembler.model.GlobalVariable;
import net.haspamelodica.javazmach.assembler.model.HeaderEntry;
import net.haspamelodica.javazmach.assembler.model.Label;
import net.haspamelodica.javazmach.assembler.model.LabelDeclaration;
import net.haspamelodica.javazmach.assembler.model.LocalVariable;
import net.haspamelodica.javazmach.assembler.model.Operand;
import net.haspamelodica.javazmach.assembler.model.StackPointer;
import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.instructions.OpcodeForm;
import net.haspamelodica.javazmach.core.instructions.OpcodeKind;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;


public class ZAssembler
{
	private static final Set<HeaderField> AUTO_FIELDS = Set.of(HeaderField.FileLength);

	private final int					version;
	private final Map<String, Opcode>	opcodesByNameLowercase;

	private final NoRangeCheckMemory	header;
	private final List<Reference>		references;

	private final Set<HeaderField>	setFields;
	private final Set<HeaderField>	partiallySetBitfields;

	private final NoRangeCheckMemory			code;
	private final SequentialMemoryWriteAccess	codeSeq;
	private final Map<String, Integer>			codeLabelRelAddrs;

	public ZAssembler(int version)
	{
		this.version = version;
		this.opcodesByNameLowercase = Arrays
				.stream(Opcode.values())
				.filter(o -> o != Opcode._unknown_instr)
				.filter(o -> version >= o.minVersion)
				.filter(o -> version <= o.maxVersion || o.maxVersion <= 0)
				// careful: don't use method "name()", but member "name".
				.collect(Collectors.toUnmodifiableMap(o -> o.name.toLowerCase(), o -> o));

		this.header = new NoRangeCheckMemory();
		this.references = new ArrayList<>();
		this.code = new NoRangeCheckMemory();
		this.codeSeq = new SequentialMemoryWriteAccess(code);
		this.setFields = new HashSet<>();
		this.partiallySetBitfields = new HashSet<>();
		this.codeLabelRelAddrs = new HashMap<>();
	}

	public void add(ZAssemblerFile file)
	{
		if(file.version().isPresent() && file.version().getAsInt() != version)
			throw new IllegalArgumentException("Version mismatch");

		add(file.entries());
	}

	public void add(List<ZAssemblerFileEntry> entries)
	{
		for(ZAssemblerFileEntry entry : entries)
			add(entry);
	}

	public void add(ZAssemblerFileEntry entry)
	{
		switch(entry)
		{
			case HeaderEntry headerEntry -> add(headerEntry);
			case LabelDeclaration labelDeclaration -> add(labelDeclaration);
			case ZAssemblerInstruction instruction -> add(instruction);
		}
	}

	public void add(HeaderEntry headerEntry)
	{
		HeaderField field;
		try
		{
			field = HeaderField.valueOf(headerEntry.name());
		} catch(IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Unknown header field: " + headerEntry.name());
		}

		boolean isBitfieldEntry = field.bitfield != null;
		if(isBitfieldEntry)
			partiallySetBitfields.add(field.bitfield);

		if(version < field.minVersion || (field.maxVersion > 0 && version > field.maxVersion))
			throw new IllegalArgumentException("Field " + field + " does not exist in version " + version
					+ "; only V" + field.minVersion +
					(field.maxVersion <= 0 ? "+" : field.maxVersion != field.minVersion ? "-" + field.maxVersion : ""));
		if(!setFields.add(field))
			System.err.println("WARNING: Field " + field + " set twice - old value will be overwritten");
		if(field.isBitfield && partiallySetBitfields.contains(field))
			System.err.println("WARNING: Bitfield " + field
					+ " is set after some bitfield entries have been set - old bitfield entry values will be overwritten");
		if(field.isRst)
			System.err.println("INFO: Field " + field + " is Rst - will usually be overwritten by interpreter on startup");

		if(AUTO_FIELDS.contains(field))
			System.err.println("INFO: Automatically computed value of " + field + " is overwritten with explicit value");

		switch(headerEntry.value())
		{
			case ConstantInteger constant ->
			{
				if(!isBitfieldEntry)
				{
					checkBigintMaxByteCount(field.len, constant.value(), bigint -> "constant out of range: "
							+ bigint + " for field " + field);
					byte[] valueBytes = constant.value().toByteArray();
					int padding = field.len - valueBytes.length;
					if(padding != 0)
					{
						byte[] valueBytesOrig = valueBytes;
						valueBytes = new byte[field.len];
						System.arraycopy(valueBytesOrig, 0, valueBytes, padding, valueBytesOrig.length);
						if(constant.value().signum() < 0)
							Arrays.fill(valueBytes, 0, padding, (byte) -1);
					}
					HeaderParser.setFieldUncheckedBytes(header, field, valueBytes, 0, field.len);
				} else
				{
					if(constant.value().signum() != 0 && !constant.value().equals(BigInteger.ONE))
						throw new IllegalArgumentException("Value of bitfield entry is neither 0 nor 1: field "
								+ field + ", value " + constant.value());

					HeaderParser.setFieldUnchecked(header, field, constant.value().testBit(0) ? 1 : 0);
				}
			}
			case ConstantByteSequence constant ->
			{
				int length = constant
						.entries()
						.stream()
						.mapToInt(e -> switch(e)
						{
							case ConstantInteger elementInteger -> 1;
							case ConstantString elementString -> elementString.value().length();
							case ConstantChar elementChar -> 1;
						})
						.sum();
				byte[] value = new byte[length];
				int i = 0;
				for(ConstantByteSequenceElement elementUncasted : constant.entries())
					switch(elementUncasted)
					{
						case ConstantInteger element ->
						{
							checkBigintMaxByteCount(1, element.value(), bigint -> "byte constant out of range: "
									+ bigint + " for field " + field);
							value[i ++] = element.value().byteValue();
						}
						case ConstantString element ->
						{
							System.arraycopy(element.value().getBytes(StandardCharsets.US_ASCII), 0, value, 0, element.value().length());
							i += element.value().length();
						}
						case ConstantChar element ->
						{
							if((element.value() & ~0x7f) != 0)
								throw new IllegalArgumentException("char constant out of range (not ASCII): " + element.value()
										+ " for field " + field);
							value[i ++] = (byte) element.value();
						}
					};

				if(isBitfieldEntry)
					throw new IllegalArgumentException("Setting a bitfield entry to a byte sequence "
							+ "(not a single integer literal) is nonsensical: " + field);

				if(field.len > value.length)
					System.err.println("WARNING: Byte sequence value for field " + field + " is too short ("
							+ value.length + "<" + field.len + "); will be padded with nullbytes");
				else if(field.len < value.length)
					throw new IllegalArgumentException("Byte sequence value for field " + field + " is too long: "
							+ value.length + ">" + field.len);

				HeaderParser.setFieldUncheckedBytes(header, field, value);
			}
			case Label label ->
			{
				if(isBitfieldEntry)
					throw new IllegalArgumentException("Setting a bitfield entry to a label is nonsensical");
				references.add(new Reference(new HeaderFieldReferenceSource(field), new CodeLabelReferenceTarget(label.name())));
			}
		}
	}

	public void add(LabelDeclaration labelDeclaration)
	{
		addCodeLabelHere(labelDeclaration.name());
	}

	public void add(ZAssemblerInstruction instruction)
	{
		Opcode opcode = opcodesByNameLowercase.get(instruction.name().toLowerCase());
		if(opcode == null)
			// shouldn't really matter - the grammar knows which opcodes there are.
			// Still, better safe than sorry - ZAssembler might theoretically be used without ZAssemblerParser,
			// and the way instructions are parsed also might change later.
			throw new IllegalArgumentException("Opcode " + instruction.name() + " unknown");
		if(opcode.isStoreOpcode != instruction.storeTarget().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is store, but no store target was given: " + instruction);
		if(opcode.isBranchOpcode != instruction.branchInfo().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is branch, but no branch info was given: " + instruction);
		if(opcode.isTextOpcode != instruction.text().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is text, but no text was given: " + instruction);

		List<Operand> operands = instruction.operands();
		//TODO only warn instead; do a check failing hard once form is known
		if(operands.size() < opcode.minArgs || operands.size() > opcode.maxArgs)
			throw new IllegalArgumentException("Incorrect number of arguments given for opcode " + opcode
					+ ": expected " + opcode.minArgs + (opcode.maxArgs != opcode.minArgs ? "-" + opcode.maxArgs : "")
					+ ", but was " + operands.size() + ": " + instruction);

		OpcodeForm form = switch(opcode.range)
		{
			case OP0 -> OpcodeForm.SHORT;
			case OP1 -> OpcodeForm.SHORT;
			case OP2 -> true
					&& instruction.form().orElse(OpcodeForm.LONG) == OpcodeForm.LONG
				// yes, we need to check this even though we know the form is OP2:
				// for example, je is OP2, but can take any number between 2 and 4 of operands.
					&& operands.size() == 2
					&& operands.get(0).isTypeEncodeableUsingOneBit()
					&& operands.get(1).isTypeEncodeableUsingOneBit()
							? OpcodeForm.LONG
							: OpcodeForm.VARIABLE;
			case VAR -> OpcodeForm.VARIABLE;
			case EXT -> OpcodeForm.EXTENDED;
		};

		if(instruction.form().isPresent() && instruction.form().get() != form)
			throw new IllegalArgumentException("Illegal form requested for opcode " + opcode
					+ ": opcode is kind " + opcode.range + ", but requested was form " + instruction.form().get());

		// There are no opcodes which would trigger this, but let's be paranoid.
		checkOpcodeNumberMask(opcode, switch(form)
		{
			case LONG, VARIABLE -> 0x1f;
			case SHORT -> 0x0f;
			case EXTENDED -> 0xff;
		}, form);

		switch(form)
		{
			case LONG -> codeSeq.writeNextByte(0
					// form LONG: bit 7 is 0.
					| (0 << 7)
					// kind: implicitly OP2.
					// operand type 1: bit 6
					| (operands.get(0).encodeTypeOneBit() << 6)
					// operand type 2: bit 5
					| (operands.get(1).encodeTypeOneBit() << 5)
					// opcode: bits 4-0.
					| (opcode.opcodeNumber << 0));
			case SHORT ->
			{
				throw new UnsupportedOperationException("Can't assemble form SHORT yet");
			}
			case EXTENDED ->
			{
				throw new UnsupportedOperationException("Can't assemble from EXTENDED yet");
			}
			case VARIABLE ->
			{
				codeSeq.writeNextByte(0
						// form VARIABLE: bits 7-6 are 0b11.
						| (0b11 << 6)
						// kind: bit 5; OP2 is 0, VAR is 1.
						| ((opcode.range == OpcodeKind.VAR ? 1 : 0) << 5)
						// opcode: bits 4-0.
						| (opcode.opcodeNumber << 0));

				int operandTypesEncoded = 0;
				int i;
				for(i = 0; i < operands.size(); i ++)
					operandTypesEncoded = (operandTypesEncoded << 2) | operands.get(i).encodeTypeTwoBits();
				// the rest is omitted, which is encoded as 0b11
				for(; i < (opcode.hasTwoOperandTypeBytes ? 8 : 4); i ++)
					operandTypesEncoded = (operandTypesEncoded << 2) | 0b11;

				if(opcode.hasTwoOperandTypeBytes)
					codeSeq.writeNextWord(operandTypesEncoded);
				else
					codeSeq.writeNextByte(operandTypesEncoded);
			}
		}

		operands.forEach(this::appendOperand);
		instruction.storeTarget().ifPresent(storeTarget -> codeSeq.writeNextByte(varnumByteAndUpdateRoutine(storeTarget)));
		instruction.branchInfo().ifPresent(branchInfo ->
		{
			//TODO branch info - this is hard because whether we can assemble this in the short form
			// depends on where the label refers to, and for the labels where it matters this even
			// will only become known in the future.
			//TODO also maybe give programmer the possibility to choose which encoding is used?
		});
	}

	private void checkOpcodeNumberMask(Opcode opcode, int mask, OpcodeForm form)
	{
		if((opcode.opcodeNumber & mask) != opcode.opcodeNumber)
			throw new IllegalArgumentException("Opcode " + opcode
					+ " should be assembled as " + form + ", but has an opcode number greater than 0x"
					+ Integer.toHexString(mask) + ": " + opcode.opcodeNumber);
	}

	private void appendOperand(Operand operand)
	{
		switch(operand)
		{
			case ConstantInteger constant ->
			{
				BigInteger value = constant.value();
				if(constant.isSmallConstant())
					codeSeq.writeNextByte(value.byteValue());
				else
				{
					ZAssemblerUtils.checkBigintMaxByteCount(2, constant.value(), v -> "Immediate operand too large : " + v);
					codeSeq.writeNextWord(constant.value().shortValue());
				}
			}
			case Variable variable -> codeSeq.writeNextByte(varnumByteAndUpdateRoutine(variable));
		};
	}

	private int varnumByteAndUpdateRoutine(Variable variable)
	{
		return switch(variable)
		{
			case StackPointer var -> 0;
			case LocalVariable var ->
			{
				if(var.index() < 0 || var.index() > 0x0f)
					throw new IllegalArgumentException("Local variable out of range: " + var.index());
				System.err.println("WARNING: local variable indices not yet checked against routine");
				//TODO check against current routine once those are implemented
				//TODO update routine once implemented
				yield var.index() + 0x1;
			}
			case GlobalVariable var ->
			{
				if(var.index() < 0 || var.index() > 0xef)
					throw new IllegalArgumentException("Global variable out of range: " + var.index());
				yield var.index() + 0x10;
			}
		};
	}

	public byte[] assemble()
	{
		preAssembleHeaderSection();
		preAssembleCodeSection();

		// compute where each section will end up
		int headerStart = 0;
		int headerEnd = headerStart + header.currentSize();
		int codeStart = headerEnd;
		int codeEnd = codeStart + code.currentSize();
		int storyfileSize = codeEnd;

		references.forEach(ref ->
		{
			int value = switch(ref.referent())
			{
				case SimpleReferenceTarget referent -> switch(referent)
				{
					case FileLengthForHeader -> switch(version)
					{
						case 1, 2, 3 -> storyfileSize / 2;
						case 4, 5 -> storyfileSize / 4;
						case 6, 7, 8 -> storyfileSize / 8;
						default -> throw new IllegalStateException("Unknown version: " + version + "; don't know how file length is stored");
					};
				};
				case CodeLabelReferenceTarget referent -> codeLabelRelAddrs.get(referent.label()) + codeStart;
			};

			switch(ref.referrer())
			{
				case HeaderFieldReferenceSource referrer -> HeaderParser.setFieldUnchecked(header, referrer.field(), value);
			}
		});

		byte[] result = new byte[storyfileSize];
		System.arraycopy(header.data(), 0, result, headerStart, header.currentSize());
		System.arraycopy(code.data(), 0, result, codeStart, code.currentSize());
		return result;
	}

	private void preAssembleHeaderSection()
	{
		for(HeaderField automaticField : AUTO_FIELDS)
			if(!setFields.contains(automaticField))
				switch(automaticField)
				{
					case FileLength -> references.add(new Reference(new HeaderFieldReferenceSource(HeaderField.FileLength), SimpleReferenceTarget.FileLengthForHeader));
					default -> throw new IllegalStateException("Field " + automaticField
							+ " is marked as auto, but is not!? This is an assembler bug.");
				}

		// ensure header is at least 0x40 / 64byte in size (by padding with nullbytes)
		if(header.currentSize() < 0x40)
			header.writeByte(0x3f, 0);

		List<HeaderField> unsetHeaderFields = Arrays.stream(HeaderField.values())
				.filter(f -> version >= f.minVersion)
				.filter(f -> f.maxVersion <= 0 || version <= f.maxVersion)
				.filter(f -> !f.isRst)
				.filter(f -> !AUTO_FIELDS.contains(f))
				.filter(f -> !setFields.contains(f))
				.filter(f -> f.bitfield == null || !setFields.contains(f.bitfield))
				.toList();

		Map<HeaderField, String> unsetBitfieldEntries = unsetHeaderFields
				.stream()
				.filter(f -> f.isBitfield)
				.collect(Collectors.toMap(f -> f, bitfield -> unsetHeaderFields
						.stream()
						.filter(f -> f.bitfield == bitfield)
						.map(HeaderField::name)
						.collect(Collectors.joining(","))));

		String unsetHeaderFieldsStr = unsetHeaderFields.stream()
				.filter(f -> f.bitfield == null)
				.filter(f -> !f.isBitfield || !unsetBitfieldEntries.get(f).isEmpty())
				.map(f -> f.isBitfield ? f.name() + "[" + unsetBitfieldEntries.get(f) + "]" : f.name())
				.collect(Collectors.joining(", "));

		if(!unsetHeaderFieldsStr.isEmpty())
			System.err.println("INFO: The following non-Rst header fields have no explicit value and will default to 0: " + unsetHeaderFieldsStr);
	}

	private void preAssembleCodeSection()
	{
		for(Reference ref : references)
			if(ref.referent() instanceof CodeLabelReferenceTarget referent)
				if(!codeLabelRelAddrs.containsKey(referent.label()))
					throw new IllegalArgumentException("Undefined label: " + referent.label());
	}

	private void addCodeLabelHere(String label)
	{
		addCodeLabel(label, code.currentSize());
	}

	private void addCodeLabel(String label, int relativeAddress)
	{
		Integer old = codeLabelRelAddrs.put(label, relativeAddress);
		if(old != null)
			throw new IllegalArgumentException("Duplicate label: " + label + " (relative " + old + " vs. " + relativeAddress + ")");
	}

	public static byte[] assemble(ZAssemblerFile file, int externallyGivenVersion, String externallyGivenVersionSourceName)
	{
		int version;
		if(externallyGivenVersion <= 0)
			version = file.version().orElseThrow(() -> new IllegalArgumentException(
					"Z-version not given: neither by " + externallyGivenVersionSourceName + ", nor by .ZVERSION in file"));
		else if(file.version().isEmpty())
			version = externallyGivenVersion;
		else if(file.version().getAsInt() == externallyGivenVersion)
			version = externallyGivenVersion;
		else
			throw new IllegalArgumentException("Z-Version given by " + externallyGivenVersionSourceName + " mismatches .ZVERSION in file");

		ZAssembler assembler = new ZAssembler(version);
		assembler.add(file);
		return assembler.assemble();
	}
}