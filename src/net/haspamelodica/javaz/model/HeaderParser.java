package net.haspamelodica.javaz.model;

import net.haspamelodica.javaz.model.memory.WritableMemory;

public class HeaderParser
{
	/** <code>V1+      </code> */
	public static final int	VersionLoc				= 0x00;
	/** <code>V1+ bitfield</code> */
	public static final int	Flags1Loc				= 0x01;
	/** <code>V5+    IR</code> */
	public static final int	ColorsAvailLoc			= 0x00_01_01;
	/** <code>V1-3     </code> */
	public static final int	StatLineTypeLoc			= 0x01_01_01;
	/** <code>V6+    IR</code> */
	public static final int	PicDisplayingAvailLoc	= 0x01_01_01;
	/** <code>V1-3     </code> */
	public static final int	StoryfileSplitLoc		= 0x02_01_01;
	/** <code>V4+    IR</code> */
	public static final int	BoldfaceAvailLoc		= 0x02_01_01;
	/** <code>V3   C I </code> */
	public static final int	TandyLoc				= 0x03_01_01;
	/** <code>V4+    IR</code> */
	public static final int	ItalicAvailLoc			= 0x03_01_01;
	/** <code>V1-3   IR</code> */
	public static final int	StatLineNotAvailLoc		= 0x04_01_01;
	/** <code>V4+    IR</code> */
	public static final int	FixedSpaceAvailLoc		= 0x04_01_01;
	/** <code>V1-3   IR</code> */
	public static final int	ScrSplitAvailLoc		= 0x05_01_01;
	/** <code>V6+    IR</code> */
	public static final int	SoundFXAvailLoc			= 0x05_01_01;
	/** <code>V1-3   IR</code> */
	public static final int	VarPitchFontDefaultLoc	= 0x06_01_01;
	/** <code>V4+    IR</code> */
	public static final int	TimedKeyInputAvailLoc	= 0x07_01_01;
	/** <code>V1+  C   </code> */
	public static final int	ReleaseNumberLoc		= 0x02_02;
	/** <code>V1+      </code> */
	public static final int	HighMemoryBaseLoc		= 0x02_04;
	/** <code>V1-5     </code> */
	public static final int	InitialPCLoc			= 0x02_06;
	/** <code>V6+      </code> */
	public static final int	MainLocLoc				= 0x02_06;
	/** <code>V1+      </code> */
	public static final int	DictionaryLocLoc		= 0x02_08;
	/** <code>V1+      </code> */
	public static final int	ObjTableLocLoc			= 0x02_0A;
	/** <code>V1+      </code> */
	public static final int	GlobalVarTableLocLoc	= 0x02_0C;
	/** <code>V1+      </code> */
	public static final int	StaticMemBaseLoc		= 0x02_0E;
	/**
	 * <code>V1+ bitfield</code><br>
	 * Note: An interpreter should clear bits 9-F on reset for forward compilance.
	 */
	public static final int	Flags2Loc				= 0x02_10;
	/** <code>V1+   DIR</code> */
	public static final int	TransciptingLoc			= 0x00_03_10;
	/** <code>V3+   D R</code> */
	public static final int	ForceFixedPitchPrintLoc	= 0x01_03_10;
	/** <code>V6+   DI </code> */
	public static final int	ScrRedrawControlLoc		= 0x02_03_10;
	/** <code>V5+    IR</code> */
	public static final int	PicsReqLoc				= 0x03_03_10;
	/** <code>V5+    IR</code> */
	public static final int	UndoReqLoc				= 0x04_03_10;
	/** <code>V5+    IR</code> */
	public static final int	MouseReqLoc				= 0x05_03_10;
	/** <code>V5+      </code> */
	public static final int	ColorsReqLoc			= 0x06_03_10;
	/** <code>V5+    IR</code> */
	public static final int	SoundFXReqLoc			= 0x07_03_10;
	/** <code>V6+    IR</code> */
	public static final int	MenusReqLoc				= 0x08_03_10;
	/** <code>V2   C   </code> */
	public static final int	SerialCodeFirstByte		= 0x12;
	public static final int	SerialCodeLength		= 6;
	/** <code>V3+  C   </code> */
	public static final int	SerialNumFirstByte		= 0x12;
	public static final int	SerialNumLength			= 6;
	/** <code>V2+      </code> */
	public static final int	AbbrevTableLocLoc		= 0x02_18;
	/** <code>V3+      </code> */
	public static final int	FileLengthLoc			= 0x02_1A;
	/** <code>V3+      </code> */
	public static final int	FileChecksumLoc			= 0x02_1C;
	/** <code>V4+    IR</code> */
	public static final int	InterpreterNumLoc		= 0x1E;
	/** <code>V4+    IR</code> */
	public static final int	InterpreterVerLoc		= 0x1F;
	/** <code>V4+    IR</code> */
	public static final int	ScrHeightLinesLoc		= 0x20;
	/** <code>V4+    IR</code> */
	public static final int	ScrWidthCharsLoc		= 0x21;
	/** <code>V5+    IR</code> */
	public static final int	ScrWidthUnitsLoc		= 0x02_22;
	/** <code>V5+    IR</code> */
	public static final int	ScrHeightUnitsLoc		= 0x02_24;
	/** <code>V5     IR</code> */
	public static final int	FontWidthV5Loc			= 0x26;
	/** <code>V6+    IR</code> */
	public static final int	FontHeightV6Loc			= 0x26;
	/** <code>V5     IR</code> */
	public static final int	FontHeightV5Loc			= 0x27;
	/** <code>V6+    IR</code> */
	public static final int	FontWidthV6Loc			= 0x27;
	/** <code>V6+      </code> */
	public static final int	RoutinesOffLoc			= 0x02_28;
	/** <code>V6+      </code> */
	public static final int	StringsOffLoc			= 0x02_2A;
	/** <code>V5+    IR</code> */
	public static final int	DefaultBGColLoc			= 0x2C;
	/** <code>V5+    IR</code> */
	public static final int	DefaultFGColLoc			= 0x2D;
	/** <code>V5+      </code> */
	public static final int	TermCharsTableLocLoc	= 0x02_2E;
	/** <code>V6+    I </code> */
	public static final int	OutSt3WidthPixelsLoc	= 0x02_30;
	/** <code>V1+    IR</code> */
	public static final int	StandardRevLoc			= 0x02_32;
	/** <code>V5+      </code> */
	public static final int	AlphabetTableLocLoc		= 0x02_34;
	/** <code>V5+      </code> */
	public static final int	HeaderExtTableLocLoc	= 0x02_36;
	/** <code>V6+  C   </code> */
	public static final int	PlayerNameFirstByte		= 0x38;
	public static final int	PlayerNameLength		= 8;
	/** <code>Vn/a C   </code> */
	public static final int	CompilerVerFirstByte	= 0x3C;
	public static final int	CompilerVerLength		= 4;

	private final WritableMemory mem;

	public HeaderParser(WritableMemory memory)
	{
		this.mem = memory;
	}

	/**
	 * It is recommended to use constants of this class for field access.
	 * <p>
	 * <code>location</code> is in following format:
	 * 
	 * <pre>
	 * 0000 0000  0000 bbbb  0000 00wf  aaaa aaaa
	 * </pre>
	 * 
	 * <code>w</code>: 0 for byte field, 1 for word field<br>
	 * <code>f</code>: 0 for whole byte/word field, 1 for single bit.<br>
	 * <code>b</code>: bit address. <code>0x0</code> for byte/word fields.<br>
	 * <code>a</code>: address of field (as byte address)
	 */
	public int getField(int location)
	{
		int isWord = location & 0x02_00;
		int addr = location & 0xFF;
		int isBitfield = location & 0x01_00;
		int bitAddr = (location & 0x0F_00_00) >>> 16;

		int byteOrWord;
		if(isWord == 0)
			byteOrWord = mem.readByte(addr);
		else
			byteOrWord = mem.readWord(addr);

		if(isBitfield == 0)
			return byteOrWord;
		else
			return (byteOrWord >>> bitAddr) & 1;
	}
	/**
	 * It is recommended to use constants of this class for field access.
	 * <p>
	 * For a description of <code>location</code>, see {@link #getField(int)}.
	 */
	public void setField(int location, int val)
	{
		int isWord = location & 0x02_00;
		int addr = location & 0xFF;
		int isBitfield = location & 0x01_00;
		int bitAddr = (location & 0x0F_00_00) >>> 16;

		int byteOrWord;
		if(isBitfield == 0)
			byteOrWord = val;
		else
		{
			if(isWord == 0)
				byteOrWord = mem.readByte(addr);
			else
				byteOrWord = mem.readWord(addr);
			byteOrWord |= (val & 1) << bitAddr;
		}

		if(isWord == 0)
			mem.writeByte(addr, byteOrWord);
		else
			mem.writeWord(addr, byteOrWord);
	}
	//TODO multi-byte fields? (Compiler version...)
}