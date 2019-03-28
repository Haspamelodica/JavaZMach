package net.haspamelodica.javaz.model;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.model.instructions.DecodedInstruction;
import net.haspamelodica.javaz.model.instructions.InstructionDecoder;
import net.haspamelodica.javaz.model.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.model.memory.SequentialMemoryAccess;
import net.haspamelodica.javaz.model.memory.WritableMemory;
import net.haspamelodica.javaz.model.stack.CallStack;

public class ZInterpreter
{
	private final int version;

	private final WritableMemory			dynamicMem;
	private final ReadOnlyMemory			mem;
	private final CallStack					stack;
	private final HeaderParser				headerParser;
	private final SequentialMemoryAccess	memAtPC;
	private final InstructionDecoder		instrDecoder;

	private int	r_o_8;
	private int	s_o_8;

	private final DecodedInstruction	currentInstr;
	private final int[]					variablesInitialValuesBuf;

	public ZInterpreter(GlobalConfig config, int versionOverride, WritableMemory dynamicMem, ReadOnlyMemory mem)
	{
		this.headerParser = new HeaderParser(dynamicMem);
		this.version = versionOverride > 0 ? versionOverride : headerParser.getField(HeaderParser.VersionLoc);
		this.dynamicMem = dynamicMem;
		this.mem = mem;
		this.stack = new CallStack();
		this.memAtPC = new SequentialMemoryAccess(mem);
		this.instrDecoder = new InstructionDecoder(config, version, memAtPC);
		this.currentInstr = new DecodedInstruction();
		this.variablesInitialValuesBuf = new int[16];
	}

	public void reset()
	{
		if(version == 6 || version == 7)
		{
			r_o_8 = 8 * headerParser.getField(HeaderParser.RoutinesOffLoc);
			s_o_8 = 8 * headerParser.getField(HeaderParser.StringsOffLoc);
		}
		if(version == 6)
			doCallTo(headerParser.getField(HeaderParser.MainLocLoc), 0, true, 0);
		else
			memAtPC.setAddress(headerParser.getField(HeaderParser.InitialPCLoc));
	}
	/**
	 * Returns true if the game should continue (=is not finished)
	 */
	public boolean step()
	{
		instrDecoder.decode(currentInstr);
		switch(currentInstr.opcode)
		{
			default:
				throw new IllegalStateException("Instruction not yet implemented: " + currentInstr.opcode);
		}
	}

	public void doCallTo(int packedRoutineAddress, int suppliedArgumentCount, boolean discardReturnValue, int storeTarget)
	{
		int returnPC = memAtPC.getAddress();
		memAtPC.setAddress(packedToByteAddr(packedRoutineAddress, true));
		int variablesCount = memAtPC.readNextByte();
		//TODO check var count
		if(version < 5)
			for(int i = 0; i < variablesCount; i ++)
				variablesInitialValuesBuf[i] = memAtPC.readNextWord();
		//Otherwise, the initial values are 0.
		//But since version is final, variablesInitialValuesBuf can only contain 0 at this point.
		//Thus, we don't need to set variablesInitialValues to 0.
		stack.pushCallFrame(returnPC, variablesCount, variablesInitialValuesBuf, suppliedArgumentCount, discardReturnValue, storeTarget);
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