package net.haspamelodica.javazmach.assembler.core;

import static net.haspamelodica.javazmach.assembler.core.SimpleReferenceTarget.FileLengthForHeader;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintBytesChecked;
import static net.haspamelodica.javazmach.assembler.core.ZAssemblerUtils.bigintIntChecked;
import static net.haspamelodica.javazmach.core.header.HeaderField.FileLength;
import static net.haspamelodica.javazmach.core.header.HeaderField.Version;
import static net.haspamelodica.javazmach.core.instructions.Opcode._unknown_instr;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.EXTENDED;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.LONG;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.SHORT;
import static net.haspamelodica.javazmach.core.instructions.OpcodeForm.VARIABLE;
import static net.haspamelodica.javazmach.core.instructions.OpcodeKind.VAR;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.haspamelodica.javazmach.assembler.model.BranchInfo;
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
import net.haspamelodica.javazmach.assembler.model.SimpleBranchTarget;
import net.haspamelodica.javazmach.assembler.model.StackPointer;
import net.haspamelodica.javazmach.assembler.model.Variable;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFile;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerFileEntry;
import net.haspamelodica.javazmach.assembler.model.ZAssemblerInstruction;
import net.haspamelodica.javazmach.assembler.model.ZString;
import net.haspamelodica.javazmach.assembler.model.ZStringElement;
import net.haspamelodica.javazmach.core.header.HeaderField;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.instructions.OpcodeForm;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryWriteAccess;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverterNoSpecialChars;
import net.haspamelodica.javazmach.core.text.ZCharsAlphabetTableDefault;
import net.haspamelodica.javazmach.core.text.ZSCIICharZCharConverter;

public class ZAssembler
{
	private static final Set<HeaderField> AUTO_FIELDS = Set.of(FileLength, Version);

	private final int					version;
	private final Map<String, Opcode>	opcodesByNameLowercase;

	private final NoRangeCheckMemory	header;
	private final List<Reference>		references;

	private final Set<HeaderField>	setFields;
	private final Set<HeaderField>	partiallySetBitfields;

	private final NoRangeCheckMemory			code;
	private final SequentialMemoryWriteAccess	codeSeq;
	private final Map<String, CodeLocation>		codeLabelLocations;
	private final List<CodeLocation>			codeLocations;

	public ZAssembler(int version)
	{
		this.version = version;
		this.opcodesByNameLowercase = Arrays
				.stream(Opcode.values())
				.filter(o -> o != _unknown_instr)
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
		this.codeLabelLocations = new HashMap<>();
		this.codeLocations = new ArrayList<>();
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
					+ "; only " + versionRangeString(field.minVersion, field.maxVersion));
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
					byte[] valueBytes = bigintBytesChecked(field.len * 8, constant.value(), bigint -> "constant out of range: "
							+ bigint + " for field " + field);
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
						case ConstantInteger element -> value[i ++] = (byte) bigintIntChecked(8,
								element.value(), bigint -> "byte constant out of range: " + bigint + " for field " + field);
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
				references.add(new Reference(new HeaderFieldReferenceSource(field), new CodeLabelAbsoluteReference(label.name())));
			}
		}
	}

	private static String versionRangeString(int minVersion, int maxVersion)
	{
		return "V" + minVersion + (maxVersion <= 0 ? "+" : maxVersion != minVersion ? "-" + maxVersion : "");
	}

	public void add(LabelDeclaration labelDeclaration)
	{
		addCodeLabelHere(labelDeclaration.name());
	}

	public void add(ZAssemblerInstruction instruction)
	{
		Opcode opcode = opcodesByNameLowercase.get(instruction.opcode().toLowerCase());
		if(opcode == null)
		{
			String existingVersionsThisName = Arrays.stream(Opcode.values())
					.filter(opcode2 -> opcode2.name.toLowerCase().equals(instruction.opcode().toLowerCase()))
					.map(opcode2 -> versionRangeString(opcode2.minVersion, opcode2.maxVersion))
					.collect(Collectors.joining(", "));
			if(!existingVersionsThisName.isEmpty())
				throw new IllegalArgumentException("Opcode " + instruction.opcode() + " doesn't exist in V" + version
						+ ", only " + existingVersionsThisName);
			// shouldn't really be possible - the grammar knows which opcodes there are.
			// Still, better safe than sorry - ZAssembler might theoretically be used without ZAssemblerParser,
			// and the way instructions are parsed also might change later.
			throw new IllegalArgumentException("Opcode " + instruction.opcode() + " unknown");
		}
		if(opcode.isStoreOpcode != instruction.storeTarget().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is store, but no store target was given: " + instruction);
		if(opcode.isBranchOpcode != instruction.branchInfo().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is branch, but no branch info was given: " + instruction);
		if(opcode.isTextOpcode != instruction.text().isPresent())
			throw new IllegalArgumentException("Opcode " + opcode + " is text, but no text was given: " + instruction);

		List<Operand> operands = instruction.operands();

		if(operands.size() < switch(opcode.range)
		{
			case OP0, OP2, VAR, EXT -> 0;
			case OP1 -> 1;
		})
			throw new IllegalArgumentException("Too few operands for " + opcode.range + " instruction; not encodeable: " + instruction);

		if(operands.size() > switch(opcode.range)
		{
			case OP0 -> 0;
			case OP1 -> 1;
			// yes, 4 / 8 even for OP2 - 4 / 8 operands are actually encodeable for OP2.
			// Case in point: je, which is OP2, takes up to 4 operands.
			case OP2, VAR, EXT -> opcode.hasTwoOperandTypeBytes ? 8 : 4;
		})
			throw new IllegalArgumentException("Too many operands for " + opcode.range + " instruction; not encodeable: " + instruction);

		if(operands.size() < opcode.minArgs || operands.size() > opcode.maxArgs)
			System.err.println("WARNING: Incorrect number of operands given for opcode " + opcode
					+ ": expected " + opcode.minArgs + (opcode.maxArgs != opcode.minArgs ? "-" + opcode.maxArgs : "")
					+ ", but was " + operands.size() + ": " + instruction);

		OpcodeForm form = switch(opcode.range)
		{
			case OP0 -> SHORT;
			case OP1 -> SHORT;
			case OP2 -> true
					&& instruction.form().orElse(LONG) == LONG
				// yes, we need to check operand count even though we know the form is OP2:
				// for example, je is OP2, but can take any number between 1 and 4 of operands.
					&& operands.size() == 2
					&& operands.get(0).isTypeEncodeableUsingOneBit()
					&& operands.get(1).isTypeEncodeableUsingOneBit()
							? LONG
							: VARIABLE;
			case VAR -> VARIABLE;
			case EXT -> EXTENDED;
		};

		if(instruction.form().isPresent() && instruction.form().get() != form)
			throw new IllegalArgumentException("Illegal form requested: kind " + opcode.range + " opcode with "
					+ operands.size() + " operands, but requested was form " + instruction.form().get());

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
			case SHORT -> codeSeq.writeNextByte(0
					// form SHORT: bits 7-6 are 0b10.
					| (0b10 << 6)
					// kind: implicitly OP0 / OP1, depending on operand type: omitted means OP0.
					// No need to check this here; operand count is already checked above.
					// operand type (if present): bits 5-4
					| ((operands.size() == 0 ? 0b11 : operands.get(0).encodeTypeTwoBits()) << 4)
					// opcode: bits 3-0.
					| (opcode.opcodeNumber << 0));
			case EXTENDED ->
			{
				// EXTENDED form only exists in V5+, but let's rely on the Opcode enum being sane and declaring all EXT opcodes as V5+.
				codeSeq.writeNextByte(0xbe);
				codeSeq.writeNextByte(opcode.opcodeNumber);
				appendEncodedOperandTypesVar(opcode, operands);
			}
			case VARIABLE ->
			{
				codeSeq.writeNextByte(0
						// form VARIABLE: bits 7-6 are 0b11.
						| (0b11 << 6)
						// kind: bit 5; OP2 is 0, VAR is 1.
						| ((opcode.range == VAR ? 1 : 0) << 5)
						// opcode: bits 4-0.
						| (opcode.opcodeNumber << 0));
				appendEncodedOperandTypesVar(opcode, operands);
			}
		}

		operands.forEach(this::appendOperand);
		instruction.storeTarget().ifPresent(storeTarget -> codeSeq.writeNextByte(varnumByteAndUpdateRoutine(storeTarget)));
		instruction.branchInfo().ifPresent(branchInfo ->
		{
			switch(branchInfo.target())
			{
				case SimpleBranchTarget target ->
				{
					switch(target)
					{
						case rfalse -> appendEncodedBranchOffset(0, branchInfo);
						case rtrue -> appendEncodedBranchOffset(1, branchInfo);
					}
				}
				case ConstantInteger target ->
				{
					BigInteger branchTargetEncoded = target.value().add(BigInteger.TWO);
					if(branchTargetEncoded.equals(BigInteger.ZERO) || branchTargetEncoded.equals(BigInteger.ONE))
						throw new IllegalArgumentException("A branch target of " + target.value()
								+ " is not encodable as it would conflict with rtrue / rfalse");
					appendEncodedBranchOffset(bigintIntChecked(14, branchTargetEncoded,
							bte -> "Branch target out of range: " + target.value()), branchInfo);
				}
				case Label target ->
				{
					// Here, we write a dummy value as the branch target, which will later be overwritten by the reference.
					// At first, optimistically assume the branch target can be assembled in short form, so choose 0 as the dummy value.
					// If it doesn't, the second byte will be inserted later when the reference is resolved.
					// Determining this beforehand would be hard because whether we can assemble this in the short form
					// depends on where the label refers to, and for the labels where it matters this even
					// will only become known in the future.
					CodeLocation codeLocationBeforeBranchOffset = codeLocationHere();
					appendEncodedBranchOffset(0, branchInfo);
					CodeLocation codeLocationAfterBranchOffset = codeLocationHere();

					references.add(new Reference(new BranchTarget(codeLocationBeforeBranchOffset, branchInfo.branchLengthOverride()),
							new CodeLabelRelativeReference(target.name(), codeLocationAfterBranchOffset)));
				}
			};
		});

		instruction.text().ifPresent(text -> appendZString(codeSeq, text));
	}

	private void appendZString(SequentialMemoryWriteAccess target, ZString text)
	{
		List<Byte> zchars = toZChars(text);
		while(zchars.size() % 3 != 0)
			zchars.add((byte) 5);
		for(int i = 0; i < zchars.size(); i += 3)
			target.writeNextWord(0
					// at end: bit 15; 0 means no, 1 means yes
					| ((i == zchars.size() - 3 ? 1 : 0) << 15)
					// Z-char 1: bits 14-10
					| ((zchars.get(i + 0) & 0x1f) << 10)
					// Z-char 2: bits 9-5
					| ((zchars.get(i + 1) & 0x1f) << 5)
					// Z-char 3: bits 4-0
					| ((zchars.get(i + 2) & 0x1f) << 0));
	}

	private List<Byte> toZChars(ZString text)
	{
		// TODO what about custom alphabets?
		ZSCIICharZCharConverter converter = new ZSCIICharZCharConverter(version, new ZCharsAlphabetTableDefault(version));

		List<Byte> result = new ArrayList<>();
		for(ZStringElement element : text.elements())
		{
			// TODO abbreviation handling, once implemented
			element
					.string()
					.codePoints()
					.peek(cp ->
					{
						if(cp == '\r')
							System.err.println("WARNING: \\r in ZSCII text will be ignored; use \\n for linebreaks instead.");
					})
					.filter(cp -> cp != '\r')
					.map(UnicodeZSCIIConverterNoSpecialChars::unicodeToZsciiNoCR)
					.forEach(zsciiChar -> converter.translateZSCIIToZChars(zsciiChar, result::add));
		}

		return result;
	}

	private void appendEncodedOperandTypesVar(Opcode opcode, List<Operand> operands)
	{
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

	private void appendEncodedBranchOffset(int branchOffsetEncodedOrZero, BranchInfo info)
	{
		boolean isValueShort = isBranchOffsetShort(branchOffsetEncodedOrZero);
		boolean isShort;
		if(info.branchLengthOverride().isEmpty())
			isShort = isValueShort;
		else
			isShort = switch(info.branchLengthOverride().get())
			{
				case LONGBRANCH -> false;
				case SHORTBRANCH ->
				{
					if(!isValueShort)
						yield throwShortOverrideButNotShort(branchOffsetEncodedOrZero);
					yield true;
				}
			};

		codeSeq.writeNextByte(0
				// branch-on-condition-false: bit 7; on false is 0, on true is 1.
				| ((info.branchOnConditionFalse() ? 0 : 1) << 7)
				// branch offset encoding: bit 6; long is 0, short is 1.
				| ((isShort ? 1 : 0) << 6)
				| (branchOffsetEncodedOrZero >> (isShort ? 0 : 8)));
		if(!isShort)
			codeSeq.writeNextByte(branchOffsetEncodedOrZero & 0xff);
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
					codeSeq.writeNextWord(bigintIntChecked(2, constant.value(), v -> "Immediate operand too large : " + v));
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

		int headerStart;
		int headerEnd;
		int codeStart;
		int codeEnd;
		int storyfileSize;

		// Try filling out references until sizes and code locations stop changing.
		boolean sizeOrCodeLocationChanged;
		do
		{
			sizeOrCodeLocationChanged = false;
			// Assume sizes and code locations are correct now,
			// and compute where each section will end up.
			headerStart = 0;
			headerEnd = headerStart + header.currentSize();
			codeStart = headerEnd;
			codeEnd = codeStart + code.currentSize();
			storyfileSize = codeEnd;

			for(Reference ref : references)
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
					case CodeLabelAbsoluteReference referent -> codeLabelLocations.get(referent.label()).relAddr() + codeStart;
					case CodeLabelRelativeReference referent -> codeLabelLocations.get(referent.label()).relAddr() - referent.loc().relAddr();
				};

				switch(ref.referrer())
				{
					case HeaderFieldReferenceSource referrer -> HeaderParser.setFieldUnchecked(header, referrer.field(), value);
					case BranchTarget referrer ->
					{
						int valueEnc = value + 2;
						int referrerRelAddr = referrer.location().relAddr();
						int oldTargetFirstByte = code.readByte(referrerRelAddr);
						boolean oldIsShort = (oldTargetFirstByte & (1 << 6)) != 0;
						boolean newIsShort = isBranchOffsetShort(valueEnc);
						if(newIsShort && oldIsShort)
						{
							// keep branch-on-condition-false: bit 7
							// short branch offset encoding: bit 6 is 1
							code.writeByte(referrerRelAddr, (oldTargetFirstByte & (1 << 7)) | (1 << 6) | valueEnc);
						} else
						{
							if(referrer.branchLengthOverride().isPresent())
								switch(referrer.branchLengthOverride().get())
								{
									case SHORTBRANCH -> throwShortOverrideButNotShort(valueEnc);
									case LONGBRANCH ->
									{
										// nothing to do
									}
								}
							// only warn if no length override is present
							else if(newIsShort)
								System.err.println("WARNING: Required space for branch target decreased!? Keeping long encoding. "
										+ "This is probably an interpreter bug.");

							// keep branch-on-condition-false: bit 7
							// long branch offset encoding: bit 6 is 0
							code.writeByte(referrerRelAddr, (oldTargetFirstByte & (1 << 7)) | (0 << 6) | ((valueEnc >> 8) & 0x3f));
							if(oldIsShort)
							{
								sizeOrCodeLocationChanged = true;
								// This will move the code location representing the location this branch is relative to.
								insertCodeByte(referrerRelAddr + 1, valueEnc & 0xff);
							} else
								code.writeByte(referrerRelAddr + 1, valueEnc & 0xff);
						}
					}
				}
			}
		} while(sizeOrCodeLocationChanged);

		// From here on, sizes and code locations are frozen.

		byte[] result = new byte[storyfileSize];
		System.arraycopy(header.data(), 0, result, headerStart, header.currentSize());
		System.arraycopy(code.data(), 0, result, codeStart, code.currentSize());
		return result;
	}

	private boolean isBranchOffsetShort(int value)
	{
		return value == (value & ((1 << 6) - 1));
	}

	private void insertCodeByte(int relAddr, int value)
	{
		code.insertByte(relAddr, value);
		codeLocations.forEach(loc -> loc.bytesInserted(relAddr, 1));
	}

	private void preAssembleHeaderSection()
	{
		for(HeaderField automaticField : AUTO_FIELDS)
			if(!setFields.contains(automaticField))
				switch(automaticField)
				{
					case FileLength -> references.add(new Reference(new HeaderFieldReferenceSource(FileLength), FileLengthForHeader));
					case Version -> HeaderParser.setFieldUnchecked(header, Version, version);
					default -> throw new IllegalStateException("Field " + automaticField
							+ " is supposedly auto, but is not handled by the assembler!? This is an assembler bug.");
				}

		// ensure header is at least 0x40 / 64 byte in size (by padding with nullbytes)
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
			if(ref.referent() instanceof CodeLabelAbsoluteReference referent)
				if(!codeLabelLocations.containsKey(referent.label()))
					throw new IllegalArgumentException("Undefined label: " + referent.label());
	}

	private <R> R throwShortOverrideButNotShort(int branchOffsetEncodedOrZero)
	{
		throw new IllegalArgumentException("Branch target length is overridden to be short, "
				+ "but actual encoded value can't be assembled in short form: " + branchOffsetEncodedOrZero);
	}

	private void addCodeLabelHere(String label)
	{
		addCodeLabel(label, codeLocationHere());
	}

	private void addCodeLabel(String label, CodeLocation codeLocation)
	{
		CodeLocation old = codeLabelLocations.put(label, codeLocation);
		if(old != null)
			throw new IllegalArgumentException("Duplicate label: " + label);
	}

	private CodeLocation codeLocationHere()
	{
		int reladdr = code.currentSize();
		CodeLocation newCodeLocation = new CodeLocation(reladdr);
		int existingLocationIndex = Collections.binarySearch(codeLocations, newCodeLocation);
		if(existingLocationIndex >= 0)
			return codeLocations.get(existingLocationIndex);

		codeLocations.add(-existingLocationIndex - 1, newCodeLocation);
		return newCodeLocation;
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
			throw new IllegalArgumentException("Z-version given by " + externallyGivenVersionSourceName + " mismatches .ZVERSION in file");

		ZAssembler assembler = new ZAssembler(version);
		assembler.add(file);
		return assembler.assemble();
	}
}
