package net.haspamelodica.javaz.core;

import static net.haspamelodica.javaz.core.HeaderParser.FileChecksumLoc;
import static net.haspamelodica.javaz.core.HeaderParser.FileLengthLoc;
import static net.haspamelodica.javaz.core.HeaderParser.GlobalVarTableLocLoc;
import static net.haspamelodica.javaz.core.HeaderParser.InitialPCLoc;
import static net.haspamelodica.javaz.core.HeaderParser.MainLocLoc;
import static net.haspamelodica.javaz.core.HeaderParser.RoutinesOffLoc;
import static net.haspamelodica.javaz.core.HeaderParser.StringsOffLoc;
import static net.haspamelodica.javaz.core.HeaderParser.VersionLoc;

import java.util.Arrays;
import java.util.Random;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.instructions.DecodedInstruction;
import net.haspamelodica.javaz.core.instructions.InstructionDecoder;
import net.haspamelodica.javaz.core.io.IOCard;
import net.haspamelodica.javaz.core.io.VideoCardDefinition;
import net.haspamelodica.javaz.core.memory.ReadOnlyBuffer;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javaz.core.memory.WritableBuffer;
import net.haspamelodica.javaz.core.memory.WritableMemory;
import net.haspamelodica.javaz.core.objects.ObjectTree;
import net.haspamelodica.javaz.core.stack.CallStack;
import net.haspamelodica.javaz.core.text.Tokeniser;
import net.haspamelodica.javaz.core.text.ZCharsAlphabet;
import net.haspamelodica.javaz.core.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javaz.core.text.ZCharsToZSCIIConverter;
import net.haspamelodica.javaz.core.text.ZSCIICharStreamReceiver;

public class ZInterpreter
{
	private final int	version;
	private final int	checksum;

	private final boolean	logInstructions;
	private final boolean	dontIgnoreIllegalVariableCount;
	private final boolean	readMoreThan15VarsForIllegalVariableCount;
	private final boolean	dontIgnoreDiv0;

	private final HeaderParser				headerParser;
	private final WritableMemory			dynamicMem;
	private final ReadOnlyMemory			mem;
	private final CallStack					stack;
	private final SequentialMemoryAccess	memAtPC;
	private final InstructionDecoder		instrDecoder;
	private final ObjectTree				objectTree;
	private final IOCard					ioCard;
	private final ReadOnlyBuffer			rBuf;
	private final WritableBuffer			wBuf;
	private final SequentialMemoryAccess	seqMemROBuf;
	private final ZCharsAlphabet			alphabet;
	private final ZCharsToZSCIIConverter	textConvFromSeqMemROBuf;
	private final ZCharsToZSCIIConverter	textConvFromPC;
	private final ZSCIICharStreamReceiver	printZSCIITarget;
	private final Tokeniser					tokeniser;
	private final Random					trueRandom;
	private final Random					rand;

	private int	r_o_8;
	private int	s_o_8;
	private int	globalVariablesOffset;

	private final DecodedInstruction	currentInstr;
	private final int[]					variablesInitialValuesBuf;
	private final int[]					operandEvaluatedValuesBuf;
	private final StringBuilder			stringBuf;
	private int							callDepth;

	public ZInterpreter(GlobalConfig config, WritableMemory dynamicMem, ReadOnlyMemory mem, VideoCardDefinition vCardDef)
	{
		this(config, -1, dynamicMem, mem, vCardDef);
	}
	public ZInterpreter(GlobalConfig config, int versionOverride, WritableMemory dynamicMem, ReadOnlyMemory mem, VideoCardDefinition vCardDef)
	{
		this.headerParser = new HeaderParser(dynamicMem);
		this.version = versionOverride > 0 ? versionOverride : headerParser.getField(VersionLoc);

		this.logInstructions = config.getBool("interpreter.debug.logs.instructions");
		this.dontIgnoreIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.dont_ignore");
		this.readMoreThan15VarsForIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.allow_read_more_than_15_vars");
		this.dontIgnoreDiv0 = config.getBool("interpreter.dont_ignore_div0");

		this.dynamicMem = dynamicMem;
		this.mem = mem;
		this.stack = new CallStack();
		this.memAtPC = new SequentialMemoryAccess(mem);
		this.instrDecoder = new InstructionDecoder(config, version, memAtPC);
		this.objectTree = new ObjectTree(config, version, headerParser, dynamicMem);
		this.rBuf = new ReadOnlyBuffer(mem);
		this.wBuf = new WritableBuffer(dynamicMem);
		this.seqMemROBuf = new SequentialMemoryAccess(mem);
		this.alphabet = new ZCharsAlphabet(config, version, headerParser, mem);
		this.textConvFromSeqMemROBuf = new ZCharsToZSCIIConverter(config, version, headerParser, mem, alphabet, new ZCharsSeqMemUnpacker(seqMemROBuf));
		this.textConvFromPC = new ZCharsToZSCIIConverter(config, version, headerParser, mem, alphabet, new ZCharsSeqMemUnpacker(memAtPC));
		this.ioCard = new IOCard(config, version, headerParser, mem, vCardDef);
		this.printZSCIITarget = ioCard::printZSCII;
		this.tokeniser = new Tokeniser(config, version, headerParser, mem, alphabet);
		this.trueRandom = new Random();
		this.rand = new Random();

		this.checksum = calculateChecksum();//calculate before memory is changed

		this.currentInstr = new DecodedInstruction();
		this.variablesInitialValuesBuf = new int[16];
		this.operandEvaluatedValuesBuf = new int[8];
		this.stringBuf = new StringBuilder();
	}

	public void reset()
	{
		if(version == 6 || version == 7)
		{
			r_o_8 = 8 * headerParser.getField(RoutinesOffLoc);
			s_o_8 = 8 * headerParser.getField(StringsOffLoc);
		}
		globalVariablesOffset = headerParser.getField(GlobalVarTableLocLoc) - 0x20;
		stack.reset();
		objectTree.reset();
		alphabet.reset();
		textConvFromSeqMemROBuf.reset();
		textConvFromPC.reset();
		ioCard.reset();
		tokeniser.reset();
		//TODO set header fields
		if(version == 6)
			doCallTo(headerParser.getField(MainLocLoc), 0, null, 0, true, 0, false);
		else
		{
			stack.pushCallFrame(-1, 0, variablesInitialValuesBuf, 0, true, 0);
			memAtPC.setAddress(headerParser.getField(InitialPCLoc));//TODO also versions 7-8?
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
			System.out.printf("pc=%05x (to %05x): ", currentInstrPC, memAtPC.getAddress() - 1);
		}
		instrDecoder.decode(currentInstr);
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
				storeVal = mem.readWord(o0 + (o1 << 1));
				break;
			case storew:
				//TODO enforce header access rules
				dynamicMem.writeWord(o0 + (o1 << 1), o2);
				break;
			case loadb:
				storeVal = mem.readByte(o0 + o1);
				break;
			case storeb:
				//TODO enforce header access rules
				dynamicMem.writeByte(o0 + o1, o2);
				break;
			case push:
				stack.push(o0);
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
					int sp = mem.readWord(o0) + 1;
					storeVal = mem.readWord(o0 + (sp << 1));
					mem.writeWord(o0, sp);
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
				if(o1E == 0 && dontIgnoreDiv0)
					throw new ArithmeticException("Division by 0");
				storeVal = o0E / o1E;
				break;
			case mod:
				if(o1E == 0 && dontIgnoreDiv0)
					throw new ArithmeticException("Division by 0");
				storeVal = o0E % o1E;
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
				//TODO signed or unsigned?
				storeVal = o1E < 0 ? o0E >> -o1E : o0 << o1E;
				break;
			case log_shift:
				//TODO signed or unsigned?
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
				break;
			case get_next_prop:
				storeVal = objectTree.getNextProp(o0, o1);
				break;
			case get_prop_len:
				storeVal = objectTree.getPropSize(o0);
				break;
			//8.7 Windows
			//8.8 Input and output streams
			//8.9 Input
			case aread://read in zmach06e.pdf
			case sread://read in zmach06e.pdf
				//TODO show status bar
				//TODO timeouts
				wBuf.reset(o0, version < 5, 1);
				storeVal = ioCard.inputToTextBuffer(wBuf);
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
				textConvFromPC.decode(printZSCIITarget);
				break;
			case print_ret://print_rtrue in zmach06e.pdf
				textConvFromPC.decode(printZSCIITarget);
				ioCard.printZSCII(13);
				doReturn(1);
				break;
			case print_addr:
				seqMemROBuf.setAddress(o0);
				textConvFromSeqMemROBuf.decode(printZSCIITarget);
				break;
			case print_paddr:
				seqMemROBuf.setAddress(packedToByteAddr(o0, false));
				textConvFromSeqMemROBuf.decode(printZSCIITarget);
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
				textConvFromSeqMemROBuf.decode(printZSCIITarget);
				break;
			//8.11 Miscellaneous screen output
			//8.12 Sound, mouse, and menus
			//8.13 Save, restore, and undo
			//8.14 Miscellaneous
			case nop:
				break;
			case random:
				if(o0E > 0)
					storeVal = rand.nextInt(o0);
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
				return false;
			case verify:
				int expectedChecksum = headerParser.getField(FileChecksumLoc);
				branchCondition = checksum == expectedChecksum;
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
				//Sign-extend 14 to 32 bit
				memAtPC.skipBytes(((currentInstr.branchOffset - 2) << 18) >> 18);
		}
		if(logInstructions)
			System.out.println();
		return true;
	}
	private int calculateChecksum()
	{
		int fileLengthField = headerParser.getField(FileLengthLoc);
		//Actually, version 1-2 can't occur, since verify exists since V3
		int fileLengthScaleFactor = version > 5 ? 8 : (version > 3 ? 4 : (version > 2 ? 2 : 1));
		int checksum = 0;
		for(int a = fileLengthField * fileLengthScaleFactor - 1; a > 0x3F; a --)
			checksum += mem.readByte(a);
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
			return dynamicMem.readWord(globalVariablesOffset + (var << 1));
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
			dynamicMem.writeWord(globalVariablesOffset + (var << 1), val);
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
			else if(dontIgnoreIllegalVariableCount)
				throw new VariableException("Illegal variable count: " + specifiedVarCount);
			else
				variablesCount = 15;//the maximum we can supply
			suppliedArgumentCount = Math.min(suppliedArgumentCount, variablesCount);//discard last arguments if there are too many
			if(version < 5)
			{
				memAtPC.skipWords(suppliedArgumentCount);//skip overwritten initial values
				for(int i = suppliedArgumentCount; i < variablesCount; i ++)
					variablesInitialValuesBuf[i] = memAtPC.readNextWord();
				if(readMoreThan15VarsForIllegalVariableCount && specifiedVarCount >>> 4 != 0)
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