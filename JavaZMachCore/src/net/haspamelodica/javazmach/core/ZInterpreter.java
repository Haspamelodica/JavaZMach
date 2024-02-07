package net.haspamelodica.javazmach.core;

import static net.haspamelodica.javazmach.core.header.HeaderField.FileChecksum;
import static net.haspamelodica.javazmach.core.header.HeaderField.FileLength;
import static net.haspamelodica.javazmach.core.header.HeaderField.GlobalVarTableLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.InitialPC15;
import static net.haspamelodica.javazmach.core.header.HeaderField.InitialPC78;
import static net.haspamelodica.javazmach.core.header.HeaderField.MainLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.RoutinesOff;
import static net.haspamelodica.javazmach.core.header.HeaderField.StringsOff;
import static net.haspamelodica.javazmach.core.header.HeaderField.Version;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.CursorXProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.CursorYProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.TextStyleProp;

import java.util.Arrays;
import java.util.Random;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.instructions.DecodedInstruction;
import net.haspamelodica.javazmach.core.instructions.InstructionDecoder;
import net.haspamelodica.javazmach.core.instructions.Opcode;
import net.haspamelodica.javazmach.core.io.IOCard;
import net.haspamelodica.javazmach.core.io.VideoCard;
import net.haspamelodica.javazmach.core.memory.CheckedWriteMemory;
import net.haspamelodica.javazmach.core.memory.CopyOnWriteMemory;
import net.haspamelodica.javazmach.core.memory.ReadOnlyBuffer;
import net.haspamelodica.javazmach.core.memory.ReadOnlyMemory;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javazmach.core.memory.WritableUndoableBuffer;
import net.haspamelodica.javazmach.core.objects.ObjectTree;
import net.haspamelodica.javazmach.core.stack.CallStack;
import net.haspamelodica.javazmach.core.text.FixedZSCIICharStream;
import net.haspamelodica.javazmach.core.text.Tokeniser;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverter;
import net.haspamelodica.javazmach.core.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javazmach.core.text.ZCharsToZSCIIConverterStream;
import net.haspamelodica.javazmach.core.text.ZSCIICharStream;
import net.haspamelodica.javazmach.core.text.ZSCIICharStreamReceiver;
import net.haspamelodica.javazmach.core.text.ZSCIICharZCharConverter;

public class ZInterpreter
{
	private final int version;

	private final boolean	logInstructions;
	private final boolean	ignoreIllegalVariableCount;
	private final boolean	readOnlyVarsForIllegalVariableCount;
	private final boolean	ignoreDiv0;

	private final HeaderParser					headerParser;
	private final ReadOnlyMemory				storyfileROM;
	private final CopyOnWriteMemory				memUncheckedWrite;
	private final CheckedWriteMemory			memCheckedWrite;
	private final CallStack						stack;
	private final SequentialMemoryAccess		memAtPC;
	private final InstructionDecoder			instrDecoder;
	private final ObjectTree					objectTree;
	private final IOCard						ioCard;
	private final ReadOnlyBuffer				rBuf;
	private final WritableUndoableBuffer		wBuf;
	private final SequentialMemoryAccess		seqMemROBuf;
	private final ZSCIICharZCharConverter		zsciiZcharsConverter;
	private final ZCharsSeqMemUnpacker			zCharsUnpackerFromSeqMemRO;
	private final ZCharsSeqMemUnpacker			zCharsUnpackerFromPC;
	private final ZCharsToZSCIIConverterStream	textConv;
	private final ZSCIICharStream				illegalObjectZSCIIStream;
	private final ZSCIICharStreamReceiver		printZSCIITarget;
	private final Tokeniser						tokeniser;
	private final Random						trueRandom;
	private final Random						rand;

	private int	r_o_8;
	private int	s_o_8;
	private int	globalVariablesOffset;

	private final DecodedInstruction	currentInstr;
	private final int[]					variablesInitialValuesBuf;
	private final int[]					operandEvaluatedValuesBuf;
	private final StringBuilder			stringBuf;
	private int							callDepth;

	public ZInterpreter(GlobalConfig config, ReadOnlyMemory storyfileROM, VideoCard videoCard, UnicodeZSCIIConverter unicodeZSCIIConverter)
	{
		this(config, -1, storyfileROM, videoCard, unicodeZSCIIConverter);
	}
	public ZInterpreter(GlobalConfig config, int versionOverride, ReadOnlyMemory storyfileROM, VideoCard videoCard, UnicodeZSCIIConverter unicodeZSCIIConverter)
	{
		this.storyfileROM = storyfileROM;
		this.version = versionOverride > 0 ? versionOverride : HeaderParser.getFieldUnchecked(storyfileROM, Version);
		this.memUncheckedWrite = new CopyOnWriteMemory(storyfileROM);
		this.headerParser = new HeaderParser(config, version, memUncheckedWrite);
		this.memCheckedWrite = new CheckedWriteMemory(config, headerParser, memUncheckedWrite);

		this.logInstructions = config.getBool("interpreter.debug.logs.instructions");
		this.ignoreIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.ignore");
		this.readOnlyVarsForIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.read_only_15_vars");
		this.ignoreDiv0 = config.getBool("interpreter.ignore_div0");

		this.stack = new CallStack();
		this.memAtPC = new SequentialMemoryAccess(memCheckedWrite);
		this.instrDecoder = new InstructionDecoder(config, version, memAtPC);
		this.objectTree = new ObjectTree(config, version, headerParser, memCheckedWrite);
		this.rBuf = new ReadOnlyBuffer(memCheckedWrite);
		this.wBuf = new WritableUndoableBuffer(memCheckedWrite);
		this.seqMemROBuf = new SequentialMemoryAccess(memCheckedWrite);
		this.zsciiZcharsConverter = new ZSCIICharZCharConverter(config, version, headerParser, memCheckedWrite);
		this.zCharsUnpackerFromSeqMemRO = new ZCharsSeqMemUnpacker(seqMemROBuf);
		this.zCharsUnpackerFromPC = new ZCharsSeqMemUnpacker(memAtPC);
		this.textConv = new ZCharsToZSCIIConverterStream(config, version, headerParser, memCheckedWrite, zsciiZcharsConverter);
		this.illegalObjectZSCIIStream = new FixedZSCIICharStream(unicodeZSCIIConverter, "<illegal object>");
		this.ioCard = new IOCard(config, version, headerParser, memCheckedWrite, videoCard);
		this.printZSCIITarget = ioCard::printZSCII;
		this.tokeniser = new Tokeniser(config, version, headerParser, memCheckedWrite, zsciiZcharsConverter);
		this.trueRandom = new Random();
		this.rand = new Random();

		this.currentInstr = new DecodedInstruction();
		this.variablesInitialValuesBuf = new int[16];
		this.operandEvaluatedValuesBuf = new int[8];
		this.stringBuf = new StringBuilder();
	}

	public void reset()
	{
		memUncheckedWrite.reset();
		memCheckedWrite.reset();
		if(version == 6 || version == 7)
		{
			r_o_8 = 8 * headerParser.getField(RoutinesOff);
			s_o_8 = 8 * headerParser.getField(StringsOff);
		}
		globalVariablesOffset = headerParser.getField(GlobalVarTableLoc) - 0x20;
		stack.reset();
		objectTree.reset();
		zsciiZcharsConverter.reset();
		ioCard.reset();
		tokeniser.reset();
		//TODO set header fields
		if(version == 6)
			doCallTo(headerParser.getField(MainLoc), 0, null, 0, true, 0, false);
		else
		{
			stack.pushCallFrame(-1, 0, variablesInitialValuesBuf, 0, true, 0);
			if(version < 6)
				memAtPC.setAddress(headerParser.getField(InitialPC15));
			else
				memAtPC.setAddress(headerParser.getField(InitialPC78));
		}
		callDepth = 0;
		if(logInstructions)
			System.out.println("Reset complete!");
	}
	/**
	 * Returns true if the game should continue (=is not finished)
	 */
	public boolean step()
	{
		int currentInstrPC = memAtPC.getAddress();
		if(logInstructions)
		{
			for(int i = 0; i < callDepth; i ++)
				System.out.print("  ");
			System.out.printf("pc=%05x ", currentInstrPC);
		}
		instrDecoder.decode(currentInstr);
		if(logInstructions)
			System.out.printf("(to %05x): ", memAtPC.getAddress() - 1);
		for(int i = 0; i < currentInstr.operandCount; i ++)
			putRawOperandValueToBufs(currentInstr, i);

		if(logInstructions)
		{
			System.out.print(currentInstr);
			if(currentInstr.operandCount != 0)
			{
				System.out.printf("; evaluated args: 0x%04x", operandEvaluatedValuesBuf[0]);
				for(int i = 1; i < currentInstr.operandCount; i ++)
					System.out.printf(", 0x%04x", operandEvaluatedValuesBuf[i]);
			}
			// ensure that when calling quit, the trace line has a line terminator.
			if(currentInstr.opcode == Opcode.quit)
				System.out.println();
		}

		boolean doStore = currentInstr.opcode.isStoreOpcode;
		int storeVal = -1;

		boolean branchCondition = false;

		int o0 = operandEvaluatedValuesBuf[0];
		int o1 = operandEvaluatedValuesBuf[1];
		int o2 = operandEvaluatedValuesBuf[2];
		//Sign-extend 16 to 32 bit
		int o0E = (o0 << 16) >> 16;
		int o1E = (o1 << 16) >> 16;
		//Opcode ordering and section numbering according to zmach06e.pdf
		//Source: http://mirror.ifarchive.org/indexes/if-archiveXinfocomXinterpretersXspecificationXzspec02.html
		switch(currentInstr.opcode)
		{
			//8.2 Reading and writing memory
			case load:
				storeVal = readVariable(o0, true);
				break;
			case store:
				writeVariable(o0, o1, true);
				break;
			case loadw:
				storeVal = memCheckedWrite.readWord(o0 + (o1 << 1));
				break;
			case storew:
				memCheckedWrite.writeWord(o0 + (o1 << 1), o2);
				break;
			case loadb:
				storeVal = memCheckedWrite.readByte(o0 + o1);
				break;
			case storeb:
				memCheckedWrite.writeByte(o0 + o1, o2);
				break;
			case push:
				stack.push(o0);
				break;
			case pop:
				stack.pop();
				break;
			case pull_V15:
			case pull_V7:
				writeVariable(o0, stack.pop(), true);
				break;
			case pull_V6:
				if(currentInstr.operandCount == 0)
					storeVal = stack.pop();
				else
				{
					int sp = memCheckedWrite.readWord(o0) + 1;
					storeVal = memCheckedWrite.readWord(o0 + (sp << 1));
					memCheckedWrite.writeWord(o0, sp);
				}
				break;
			//8.3 Arithmetic
			case add:
				storeVal = o0 + o1;
				break;
			case sub:
				storeVal = o0 - o1;
				break;
			case mul:
				storeVal = o0E * o1E;
				break;
			case div:
				if(o1E != 0)
					storeVal = o0E / o1E;
				else if(!ignoreDiv0)
					throw new ArithmeticException("Division by 0");
				else
					storeVal = 0;
				break;
			case mod:
				if(o1E != 0)
					storeVal = o0E % o1E;
				else if(!ignoreDiv0)
					throw new ArithmeticException("Division by 0");
				else
					storeVal = 0;
				break;
			case inc:
				writeVariable(o0, readVariable(o0) + 1);
				break;
			case dec:
				writeVariable(o0, readVariable(o0) - 1);
				break;
			case inc_chk://inc_jg in zmach06e.pdf
				int newVal = readVariable(o0) + 1;
				writeVariable(o0, newVal);
				branchCondition = (newVal << 16) >> 16 > o1E;
				break;
			case dec_chk://dec_jl in zmach06e.pdf
				newVal = readVariable(o0) - 1;
				writeVariable(o0, newVal);
				branchCondition = (newVal << 16) >> 16 < o1E;
				break;
			case or:
				storeVal = o0 | o1;
				break;
			case and:
				storeVal = o0 & o1;
				break;
			case not_V14:
			case not_V5:
				storeVal = ~o0;
				break;
			case art_shift:
				storeVal = o1E < 0 ? o0E >> -o1E : o0 << o1E;
				break;
			case log_shift:
				storeVal = o1E < 0 ? o0 >>> -o1E : o0 << o1E;
				break;
			//8.4 Comparison and jumps
			case jz:
				branchCondition = o0 == 0;
				break;
			case je:
				branchCondition = false;
				for(int i = 1; i < currentInstr.operandCount; i ++)
					if(o0 == operandEvaluatedValuesBuf[i])
					{
						branchCondition = true;
						break;
					}
				break;
			case jl:
				branchCondition = o0E < o1E;
				break;
			case jg:
				branchCondition = o0E > o1E;
				break;
			case jin:
				branchCondition = objectTree.getParent(o0) == o1;
				break;
			case test:
				branchCondition = (o0 & o1) == o1;
				break;
			case jump:
				//Sign-extend 16 to 32 bit
				//TODO signed or unsigned?
				memAtPC.skipBytes(((o0E - 2) << 16) >> 16);
				break;
			//8.5 Call and return, throw and catch
			case call_1s://call_f0 in zmach06e.pdf
			case call_1n://call_p0 in zmach06e.pdf
			case call_2s://call_f1 in zmach06e.pdf
			case call_2n://call_p1 in zmach06e.pdf
			case call://call_fv in zmach06e.pdf
			case call_vs://call_fv in zmach06e.pdf
			case call_vn://call_pv in zmach06e.pdf
			case call_vs2://call_fd in zmach06e.pdf
			case call_vn2://call_pd in zmach06e.pdf
				doStore = false;//return will do this store
				int argCount = currentInstr.operandCount - 1;
				boolean discardRetVal = !currentInstr.opcode.isStoreOpcode;
				doCallTo(o0, argCount, operandEvaluatedValuesBuf, 1, discardRetVal, currentInstr.storeTarget, true);
				break;
			case ret:
				doReturn(o0);
				break;
			case rtrue:
				doReturn(1);
				break;
			case rfalse:
				doReturn(0);
				break;
			case ret_popped://ret_pulled in zmach06e.pdf
				doReturn(readVariable(0));
				break;
			case check_arg_count:
				branchCondition = stack.getCurrentCallFrameSuppliedArgumentsCount() >= o0;
				break;
			//8.6 Objects, attributes, and properties
			case get_sibling:
				storeVal = objectTree.getSibling(o0);
				branchCondition = storeVal != 0;
				break;
			case get_child:
				storeVal = objectTree.getChild(o0);
				branchCondition = storeVal != 0;
				break;
			case get_parent:
				storeVal = objectTree.getParent(o0);
				break;
			case remove_obj:
				objectTree.removeObj(o0);
				break;
			case insert_obj:
				objectTree.insertObj(o0, o1);
				break;
			case test_attr:
				branchCondition = objectTree.getAttribute(o0, o1) == 1;
				break;
			case set_attr:
				objectTree.setAttribute(o0, o1, 1);
				break;
			case clear_attr:
				objectTree.setAttribute(o0, o1, 0);
				break;
			case put_prop:
				objectTree.putPropOrThrow(o0, o1, o2);
				break;
			case get_prop:
				storeVal = objectTree.getPropOrDefault(o0, o1);
				break;
			case get_prop_addr:
				storeVal = objectTree.getPropAddr(o0, o1);
				//Error in zmach06e.pdf!
				if(storeVal == -1)
					storeVal = 0;
				break;
			case get_next_prop:
				storeVal = objectTree.getNextProp(o0, o1);
				break;
			case get_prop_len:
				//See note in section 15
				if(o0 == 0)
					storeVal = 0;
				else
					storeVal = objectTree.getPropSize(o0);
				break;
			//8.7 Windows
			case split_window://split_screen in zmach06e.pdf
				ioCard.splitScreen(o0);
				break;
			case set_window:
				ioCard.selectWindow(o0);
				if(o0 == 1)
					if(version == 3)
						ioCard.eraseWindow(1);
					else if(version > 3 && version < 6)
					{
						ioCard.setPropertyCurrentWindow(CursorXProp, 1);
						ioCard.setPropertyCurrentWindow(CursorYProp, 1);
					}
				break;
			case set_cursor:
				//TODO: "It is an error in V4-5 to use this instruction when window 0 is selected"
				if(currentInstr.operandCount > 2)
				{
					ioCard.setPropertyCurrentWindow(CursorXProp, o1);
					ioCard.setPropertyCurrentWindow(CursorYProp, o0);
				} else
				{
					ioCard.setProperty(o2, CursorXProp, o1);
					ioCard.setProperty(o2, CursorYProp, o0);
				}
				break;
			case buffer_mode:
				if(version == 4)
				{
					ioCard.setBufferMode(0, o0);
					ioCard.setBufferMode(1, o0);
				} else if(version != 6)
					ioCard.setBufferMode(0, o0);
				else
					ioCard.setBufferMode(o0);
				break;
			case set_text_style:
				if(version > 3 && version != 6)
				{
					if(o0 == 0)
					{
						ioCard.setProperty(0, TextStyleProp, 0);
						ioCard.setProperty(1, TextStyleProp, 0);
					} else
					{
						ioCard.setProperty(0, TextStyleProp, o0 | ioCard.getProperty(0, TextStyleProp));
						ioCard.setProperty(1, TextStyleProp, o0 | ioCard.getProperty(1, TextStyleProp));
					}
				} else if(o0 == 0)
					ioCard.setPropertyCurrentWindow(TextStyleProp, 0);
				else
					ioCard.setPropertyCurrentWindow(TextStyleProp, o0 | ioCard.getPropertyCurrentWindow(TextStyleProp));
				break;
			//8.8 Input and output streams
			//8.9 Input
			case aread://read in zmach06e.pdf
			case sread://read in zmach06e.pdf
				//TODO timeouts
				int locationObj = readVariable(0x10);
				ZSCIICharStream location;
				if(objectTree.isValidObjNumber(locationObj))
				{
					seqMemROBuf.setAddress(objectTree.getObjectNameLoc(locationObj));//1st global
					textConv.reset(zCharsUnpackerFromSeqMemRO);
					location = textConv;
				} else
					location = illegalObjectZSCIIStream;
				ioCard.showStatusBar(location, readVariable(0x11), readVariable(0x12));//2nd & 3rd global
				wBuf.reset(o0, version < 5, 1);
				storeVal = ioCard.inputToTextBuffer(wBuf);
				if(storeVal == -1)
					storeVal = 0;
				else if(storeVal == -2)
					storeVal = 0;
				if(o1 != 0)
				{
					rBuf.reset(o0, version < 5, 1);
					wBuf.reset(o1, false, 4);
					tokeniser.tokenise(rBuf, wBuf);
				}
				break;
			case read_char:
				storeVal = ioCard.inputSingleChar();
				break;
			//8.10 Character based output
			case print_char:
				ioCard.printZSCII(o0);
				break;
			case new_line:
				ioCard.printZSCII(13);
				break;
			case print:
				textConv.reset(zCharsUnpackerFromPC);
				textConv.decode(printZSCIITarget);
				break;
			case print_ret://print_rtrue in zmach06e.pdf
				textConv.reset(zCharsUnpackerFromPC);
				textConv.decode(printZSCIITarget);
				ioCard.printZSCII(13);
				doReturn(1);
				break;
			case print_addr:
				seqMemROBuf.setAddress(o0);
				textConv.reset(zCharsUnpackerFromSeqMemRO);
				textConv.decode(printZSCIITarget);
				break;
			case print_paddr:
				seqMemROBuf.setAddress(packedToByteAddr(o0, false));
				textConv.reset(zCharsUnpackerFromSeqMemRO);
				textConv.decode(printZSCIITarget);
				break;
			case print_num:
				//Sign-extend 16 to 32 bit
				stringBuf.append((o0E << 16) >> 16);
				for(int i = 0; i < stringBuf.length(); i ++)
					ioCard.printZSCII(stringBuf.charAt(i));
				stringBuf.setLength(0);
				break;
			case print_obj:
				seqMemROBuf.setAddress(objectTree.getObjectNameLoc(o0));
				textConv.reset(zCharsUnpackerFromSeqMemRO);
				textConv.decode(printZSCIITarget);
				break;
			//8.11 Miscellaneous screen output
			case erase_window:
				ioCard.eraseWindow(o0E);
				break;
			//8.12 Sound, mouse, and menus
			//8.13 Save, restore, and undo
			//8.14 Miscellaneous
			case nop:
				break;
			case random:
				if(o0E > 0)
					storeVal = rand.nextInt(o0) + 1;
				else
				{
					storeVal = 0;
					if(o0 == 0)
						rand.setSeed(trueRandom.nextLong());
					else
						rand.setSeed(-o0E);
				}
				break;
			case restart:
				//TODO keep "transcribe to printer" and "use fixed pitch font" bits; reset everything else
				reset();
				break;
			case quit:
				ioCard.flushBuffer();
				return false;
			case verify:
				int expectedChecksum = headerParser.getField(FileChecksum);
				branchCondition = calculateChecksum() == expectedChecksum;
				break;
			case piracy:
				branchCondition = true;//"Look! It says 'gullible' on the ceiling!" :)
				break;
			default:
				throw new IllegalStateException("Instruction not yet implemented: " + currentInstr.opcode);
		}
		if(doStore)
		{
			if(logInstructions)
				System.out.printf("; store val: 0x%04x", storeVal & 0xFFFF);
			writeVariable(currentInstr.storeTarget, storeVal);
		}
		if(currentInstr.opcode.isBranchOpcode && (branchCondition ^ currentInstr.branchOnConditionFalse))
		{
			if(logInstructions)
				System.out.print("; branch taken");
			if(currentInstr.branchOffset == 0)
				doReturn(0);
			else if(currentInstr.branchOffset == 1)
				doReturn(1);
			else
				memAtPC.skipBytes(currentInstr.branchOffset - 2);
		}
		if(logInstructions)
			System.out.println();
		return true;
	}
	private int calculateChecksum()
	{
		int fileLengthField = headerParser.getField(FileLength);
		//Actually, version 1-2 can't occur, since verify exists since V3
		//TODO z1point1 says even V1 uses scaling of 2.
		int fileLengthScaleFactor = version > 5 ? 8 : (version > 3 ? 4 : (version > 2 ? 2 : 1));
		int checksum = 0;
		for(int a = fileLengthField * fileLengthScaleFactor - 1; a > 0x3F; a --)
			checksum += storyfileROM.readByte(a);
		return checksum & 0xFFFF;
	}
	private void putRawOperandValueToBufs(DecodedInstruction instr, int i)
	{
		int specifiedVal = instr.operandValues[i];
		switch(instr.operandTypes[i])
		{
			case LARGE_CONST:
				operandEvaluatedValuesBuf[i] = specifiedVal;
				break;
			case SMALL_CONST:
				operandEvaluatedValuesBuf[i] = specifiedVal;
				//operandEvaluatedValuesBuf[i] = ((specifiedVal << 24) >> 24)&0xFFFF;
				break;
			case VARIABLE:
				operandEvaluatedValuesBuf[i] = readVariable(specifiedVal);
				break;
			default:
				throw new IllegalArgumentException("Unknown enum type: " + instr.operandTypes[i]);
		}
	}
	private int readVariable(int var, boolean var0DoesntPop)
	{
		int val = readVariable(var);
		if(var0DoesntPop && var == 0)
			stack.push(val);
		return val;
	}
	private int readVariable(int var)
	{
		if(var == 0)
			return stack.pop();
		else if(var > 0 && var < 0x10)
			return stack.readLocalVariable(var);
		else
			return memCheckedWrite.readWord(globalVariablesOffset + (var << 1));
	}
	private void writeVariable(int var, int val, boolean var0DoesntPush)
	{
		if(var0DoesntPush && var == 0)
			stack.pop();
		writeVariable(var, val);
	}
	private void writeVariable(int var, int val)
	{
		if(var == 0)
			stack.push(val);
		else if(var > 0 && var < 0x10)
			stack.writeLocalVariable(var, val);
		else
			memCheckedWrite.writeWord(globalVariablesOffset + (var << 1), val);
	}

	public void doCallTo(int packedRoutineAddress, int suppliedArgumentCount, int[] arguments, int argsOff, boolean discardReturnValue, int storeTarget, boolean callTo0Allowed)
	{
		callDepth ++;
		int returnPC = memAtPC.getAddress();
		if(packedRoutineAddress == 0)
			if(callTo0Allowed)
			{
				if(!discardReturnValue)
				{
					stack.pushCallFrame(returnPC, 0, variablesInitialValuesBuf, 0, false, storeTarget);
					doReturn(0);
				}
			} else
				throw new ControlFlowException("Call to routine at packed address 0");
		else
		{
			memAtPC.setAddress(packedToByteAddr(packedRoutineAddress, true));
			int specifiedVarCount = memAtPC.readNextByte();
			int variablesCount;
			if(specifiedVarCount >>> 4 == 0)//only the lower 4 bit are allowed to be set
				variablesCount = specifiedVarCount;
			else if(!ignoreIllegalVariableCount)
				throw new VariableException("Illegal variable count: " + specifiedVarCount);
			else
				variablesCount = 15;//the maximum we can supply
			suppliedArgumentCount = Math.min(suppliedArgumentCount, variablesCount);//discard last arguments if there are too many
			if(version < 5)
			{
				memAtPC.skipWords(suppliedArgumentCount);//skip overwritten initial values
				for(int i = suppliedArgumentCount; i < variablesCount; i ++)
					variablesInitialValuesBuf[i] = memAtPC.readNextWord();
				if(!readOnlyVarsForIllegalVariableCount && specifiedVarCount >>> 4 != 0)
					memAtPC.skipWords(specifiedVarCount - 15);
			} else
				Arrays.fill(variablesInitialValuesBuf, suppliedArgumentCount, variablesCount, 0);
			for(int i = 0; i < suppliedArgumentCount; i ++)
				variablesInitialValuesBuf[i] = arguments[i + argsOff];
			stack.pushCallFrame(returnPC, variablesCount, variablesInitialValuesBuf, suppliedArgumentCount, discardReturnValue, storeTarget);
		}
	}
	public void doReturn(int returnVal)
	{
		callDepth --;
		boolean discardReturnValue = stack.getCurrentCallFrameDiscardReturnValue();
		int storeTarget = stack.getCurrentCallFrameStoreTarget();
		memAtPC.setAddress(stack.popCallFrame());
		if(stack.getCurrentCallFrameFP() <= 0)
			throw new ControlFlowException("Return from main routine");
		if(!discardReturnValue)
			//TODO interrupt routines
			writeVariable(storeTarget, returnVal);
	}
	public int packedToByteAddr(int packed, boolean isRoutine)
	{
		switch(version)
		{
			case 1:
			case 2:
			case 3:
				return packed << 1;
			case 4:
			case 5:
				return packed << 2;
			case 6:
			case 7:
				return (packed << 2) + (isRoutine ? r_o_8 : s_o_8);
			case 8:
				return packed << 3;
			default:
				throw new VersionException("Unknown version: " + version);
		}
	}
}
