package net.haspamelodica.javaz.core;

import static net.haspamelodica.javaz.core.HeaderParser.GlobalVarTableLocLoc;
import static net.haspamelodica.javaz.core.HeaderParser.InitialPCLoc;
import static net.haspamelodica.javaz.core.HeaderParser.MainLocLoc;
import static net.haspamelodica.javaz.core.HeaderParser.RoutinesOffLoc;
import static net.haspamelodica.javaz.core.HeaderParser.StringsOffLoc;
import static net.haspamelodica.javaz.core.HeaderParser.VersionLoc;

import java.util.Arrays;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.instructions.DecodedInstruction;
import net.haspamelodica.javaz.core.instructions.InstructionDecoder;
import net.haspamelodica.javaz.core.io.IOCard;
import net.haspamelodica.javaz.core.io.VideoCardDefinition;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javaz.core.memory.WritableMemory;
import net.haspamelodica.javaz.core.objects.ObjectTree;
import net.haspamelodica.javaz.core.stack.CallStack;
import net.haspamelodica.javaz.core.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javaz.core.text.ZCharsToZSCIIConverter;

public class ZInterpreter
{
	private static boolean DEBUG_SYSOUTS = false;

	private final int version;

	private final boolean	dontIgnoreIllegalVariableCount;
	private final boolean	readMoreThan15VarsForIllegalVariableCount;

	private final HeaderParser				headerParser;
	private final WritableMemory			dynamicMem;
	private final ReadOnlyMemory			mem;
	private final CallStack					stack;
	private final SequentialMemoryAccess	memAtPC;
	private final InstructionDecoder		instrDecoder;
	private final ObjectTree				objectTree;
	private final SequentialMemoryAccess	textConvSeqMem;
	private final ZCharsToZSCIIConverter	textConv;
	private final ZCharsToZSCIIConverter	textConvFromPC;
	private final IOCard					ioCard;

	private int	r_o_8;
	private int	s_o_8;
	private int	globalVariablesOffset;

	private final DecodedInstruction	currentInstr;
	private final int[]					variablesInitialValuesBuf;
	private final int[]					operandEvaluatedValuesBufSigned;
	private final int[]					operandEvaluatedValuesBufUnsigned;
	private final StringBuilder			stringBuf;

	public ZInterpreter(GlobalConfig config, WritableMemory dynamicMem, ReadOnlyMemory mem, VideoCardDefinition vCardDef)
	{
		this(config, -1, dynamicMem, mem, vCardDef);
	}
	public ZInterpreter(GlobalConfig config, int versionOverride, WritableMemory dynamicMem, ReadOnlyMemory mem, VideoCardDefinition vCardDef)
	{
		this.headerParser = new HeaderParser(dynamicMem);
		this.version = versionOverride > 0 ? versionOverride : headerParser.getField(VersionLoc);

		this.dontIgnoreIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.dont_ignore");
		this.readMoreThan15VarsForIllegalVariableCount = config.getBool("interpreter.variables.illegal_var_count.allow_read_more_than_15_vars");

		this.dynamicMem = dynamicMem;
		this.mem = mem;
		this.stack = new CallStack();
		this.memAtPC = new SequentialMemoryAccess(mem);
		this.instrDecoder = new InstructionDecoder(config, version, memAtPC);
		this.objectTree = new ObjectTree(config, version, headerParser, dynamicMem);
		this.textConvSeqMem = new SequentialMemoryAccess(mem);
		this.textConv = new ZCharsToZSCIIConverter(config, version, headerParser, mem, new ZCharsSeqMemUnpacker(textConvSeqMem));
		this.textConvFromPC = new ZCharsToZSCIIConverter(config, version, headerParser, mem, new ZCharsSeqMemUnpacker(memAtPC));
		this.ioCard = new IOCard(config, version, headerParser, mem, vCardDef);

		this.currentInstr = new DecodedInstruction();
		this.variablesInitialValuesBuf = new int[16];
		this.operandEvaluatedValuesBufSigned = new int[8];
		this.operandEvaluatedValuesBufUnsigned = new int[8];
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
		textConv.reset();
		textConvFromPC.reset();
		ioCard.reset();
		//TODO set header fields
		if(version == 6)
			doCallTo(headerParser.getField(MainLocLoc), 0, null, 0, true, 0);
		else
		{
			stack.pushCallFrame(-1, 0, variablesInitialValuesBuf, 0, true, 0);
			memAtPC.setAddress(headerParser.getField(InitialPCLoc));//TODO also versions 7-8?
		}
		if(DEBUG_SYSOUTS)
			System.out.println("Reset complete!");
	}
	/**
	 * Returns true if the game should continue (=is not finished)
	 */
	public boolean step()
	{
		int currentInstrPC = memAtPC.getAddress();
		instrDecoder.decode(currentInstr);
		if(DEBUG_SYSOUTS)
		{
			System.out.printf("pc=%05x (to %05x): ", currentInstrPC, memAtPC.getAddress() - 1);
			System.out.println(currentInstr);
		}
		for(int i = 0; i < currentInstr.operandCount; i ++)
			putRawOperandValueToBufs(currentInstr, i);

		boolean doStore = currentInstr.opcode.isStoreOpcode;
		int storeVal = -1;

		boolean branchCondition = false;

		int o0U = operandEvaluatedValuesBufUnsigned[0];
		int o1U = operandEvaluatedValuesBufUnsigned[1];
		int o2U = operandEvaluatedValuesBufUnsigned[2];
		int o0S = operandEvaluatedValuesBufSigned[0];
		int o1S = operandEvaluatedValuesBufSigned[1];
		//Opcode ordering and section numbering according to zmach06e.pdf
		//Source: http://mirror.ifarchive.org/indexes/if-archiveXinfocomXinterpretersXspecificationXzspec02.html
		switch(currentInstr.opcode)
		{
			//8.2 Reading and writing memory
			case store:
				writeVariable(o0U, o1U);
				break;
			case loadw:
				storeVal = mem.readWord(o0U + (o1U << 1));
				break;
			case storew:
				//TODO enforce header access rules
				dynamicMem.writeWord(o0U + (o1U << 1), o2U);
				break;
			case loadb:
				storeVal = mem.readByte(o0U + o1U);
				break;
			case push:
				stack.push(o0U);
				break;
			case pull_V15:
				writeVariable(o0U, stack.pop());
				break;
			//8.3 Arithmetic
			case add:
				storeVal = o0U + o1U;
				break;
			case sub:
				storeVal = o0U - o1U;
				break;
			case mul:
				storeVal = o0U * o1U;
				break;
			case inc:
				writeVariable(o0U, readVariable(o0U) + 1);
				break;
			case inc_chk://inc_jg in zmach06e.pdf
				int oldVal = readVariable(o0U);
				writeVariable(o0U, oldVal + 1);
				//TODO read again or use old value?
				//Makes a difference for variable 0 (Stack)
				branchCondition = readVariable(o0U) > o1S;
				break;
			case dec_chk://dec_jl in zmach06e.pdf
				oldVal = readVariable(o0U);
				writeVariable(o0U, oldVal - 1);
				//TODO read again or use old value?
				//Makes a difference for variable 0 (Stack)
				branchCondition = readVariable(o0U) < o1S;
				break;
			case and:
				storeVal = o0U & o1U;
				break;
			//8.4 Comparison and jumps
			case jz:
				branchCondition = o0U == 0;
				break;
			case je:
				branchCondition = false;
				for(int i = 1; i < currentInstr.operandCount; i ++)
					if(o0U == operandEvaluatedValuesBufUnsigned[i])
					{
						branchCondition = true;
						break;
					}
				break;
			case jl:
				branchCondition = o0U < o1U;
				break;
			case jg:
				branchCondition = o0U > o1U;
				break;
			case jin:
				branchCondition = objectTree.getParent(o0U) == o1U;
				break;
			case test:
				branchCondition = (o0U & o1U) == o1U;
				break;
			case jump:
				//Sign-extend 16 to 32 bit
				//TODO signed or unsigned?
				memAtPC.skipBytes(((o0S - 2) << 16) >> 16);
				break;
			//8.5 Call and return, throw and catch
			case call:
				if(o0U == 0)
				{
					storeVal = 0;
					break;
				}
				doStore = false;//return will do this store
				doCallTo(o0U, currentInstr.operandCount - 1, operandEvaluatedValuesBufUnsigned, 1, false, currentInstr.storeTarget);
				break;
			case ret:
				doReturn(o0U);
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
			//8.6 Objects, attributes, and properties
			case get_sibling:
				storeVal = objectTree.getSibling(o0U);
				branchCondition = storeVal != 0;
				break;
			case get_child:
				storeVal = objectTree.getChild(o0U);
				branchCondition = storeVal != 0;
				break;
			case get_parent:
				storeVal = objectTree.getParent(o0U);
				break;
			case insert_obj:
				objectTree.insertObj(o0U, o1U);
				break;
			case test_attr:
				branchCondition = objectTree.getAttribute(o0U, o1U) == 1;
				break;
			case set_attr:
				objectTree.setAttribute(o0U, o1U, 1);
				break;
			case put_prop:
				objectTree.putPropOrThrow(o0U, o1U, o2U);
				break;
			case get_prop:
				storeVal = objectTree.getPropOrDefault(o0U, o1U);
				break;
			//8.7 Windows
			//8.8 Input and output streams
			case print_char:
				ioCard.printZSCII(o0U);
				break;
			case new_line:
				ioCard.printZSCII(13);
				break;
			case print:
				textConvFromPC.decode(ioCard::printZSCII);
				break;
			case print_ret://print_rtrue in zmach06e.pdf
				textConvFromPC.decode(ioCard::printZSCII);
				ioCard.printZSCII(13);
				doReturn(1);
				break;
			case print_addr:
				textConvSeqMem.setAddress(o0U);
				textConv.decode(ioCard::printZSCII);
				break;
			case print_paddr:
				textConvSeqMem.setAddress(packedToByteAddr(o0U, false));
				textConv.decode(ioCard::printZSCII);
				break;
			case print_num:
				//Sign-extend 16 to 32 bit
				stringBuf.append((o0S << 16) >> 16);
				for(int i = 0; i < stringBuf.length(); i ++)
					ioCard.printZSCII(stringBuf.charAt(i));
				stringBuf.setLength(0);
				break;
			case print_obj:
				textConvSeqMem.setAddress(objectTree.getObjectNameLoc(o0U));
				textConv.decode(ioCard::printZSCII);
				break;
			//8.9 Input
			//8.10 Character based output
			//8.11 Miscellaneous screen output
			//8.12 Sound, mouse, and menus
			//8.13 Save, restore, and undo
			//8.14 Miscellaneous
			default:
				throw new IllegalStateException("Instruction not yet implemented: " + currentInstr.opcode);
		}
		if(doStore)
			writeVariable(currentInstr.storeTarget, storeVal);
		if(currentInstr.opcode.isBranchOpcode && (branchCondition ^ currentInstr.branchOnConditionFalse))
			if(currentInstr.branchOffset == 0)
				doReturn(0);
			else if(currentInstr.branchOffset == 1)
				doReturn(1);
			else
				//Sign-extend 14 to 32 bit
				memAtPC.skipBytes(((currentInstr.branchOffset - 2) << 18) >> 18);
		return true;
	}
	private void putRawOperandValueToBufs(DecodedInstruction instr, int i)
	{
		int specifiedVal = instr.operandValues[i];
		switch(instr.operandTypes[i])
		{
			case LARGE_CONST:
				operandEvaluatedValuesBufSigned[i] = specifiedVal;
				operandEvaluatedValuesBufUnsigned[i] = specifiedVal;
				break;
			case SMALL_CONST:
				//Sign-extend 8 to 16 bit
				operandEvaluatedValuesBufSigned[i] = ((specifiedVal << 24) >> 24) & 0xFFFF;
				operandEvaluatedValuesBufUnsigned[i] = specifiedVal;
				break;
			case VARIABLE:
				int val = readVariable(specifiedVal);
				operandEvaluatedValuesBufSigned[i] = val;
				operandEvaluatedValuesBufUnsigned[i] = val;
				break;
			default:
				throw new IllegalArgumentException("Unknown enum type: " + instr.operandTypes[i]);
		}
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
	private void writeVariable(int var, int val)
	{
		if(var == 0)
			stack.push(val);
		else if(var > 0 && var < 0x10)
			stack.writeLocalVariable(var, val);
		else
			dynamicMem.writeWord(globalVariablesOffset + (var << 1), val);
	}

	public void doCallTo(int packedRoutineAddress, int suppliedArgumentCount, int[] arguments, int argsOff, boolean discardReturnValue, int storeTarget)
	{
		int returnPC = memAtPC.getAddress();
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
	public void doReturn(int returnVal)
	{
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