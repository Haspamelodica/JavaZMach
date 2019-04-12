package net.haspamelodica.javaz.core.header;

public enum HeaderField
{
	Version(0x00, 1, 0, CDIR.____, 1),
	Flags1(0x01, 1, 0, 1),
	ColorsAvail(0x00, 5, 0, CDIR.__IR, Flags1),
	StatLineType(0x01, 3, 3, CDIR.____, Flags1),
	PicDisplayingAvail(0x01, 6, 0, CDIR.__IR, Flags1),//TODO also versions 7+8?
	StoryfileSplit(0x02, 3, 3, CDIR.____, Flags1),
	BoldfaceAvail(0x02, 4, 0, CDIR.__IR, Flags1),
	Tandy(0x03, 3, 3, CDIR.C_I_, Flags1),
	ItalicAvail(0x03, 4, 0, CDIR.__IR, Flags1),
	StatLineNotAvail(0x04, 3, 3, CDIR.__IR, Flags1),
	FixedSpaceAvail(0x04, 4, 0, CDIR.__IR, Flags1),
	ScrSplitAvail(0x05, 3, 3, CDIR.__IR, Flags1),
	SoundFXAvail(0x05, 6, 0, CDIR.__IR, Flags1),//TODO also versions 7+8?
	VarPitchFontDefault(0x06, 0, 0, CDIR.__IR, Flags1),//TODO min version?
	TimedKeyInputAvail(0x07, 4, 0, CDIR.__IR, Flags1),
	ReleaseNumber(0x02, 1, 0, CDIR.C___, 2),
	HighMemoryBase(0x04, 1, 0, CDIR.____, 2),
	InitialPC15(0x06, 1, 5, CDIR.____, 2),
	MainLoc(0x06, 6, 6, CDIR.____, 2),
	InitialPC78(0x06, 7, 0, CDIR.____, 2),
	DictionaryLoc(0x08, 1, 0, CDIR.____, 2),
	ObjTableLoc(0x0A, 1, 0, CDIR.____, 2),
	GlobalVarTableLoc(0x0C, 1, 0, CDIR.____, 2),
	StaticMemBase(0x0E, 1, 0, CDIR.____, 2),
	/** Note: An interpreter should clear bits 9-F on reset for forward compilance. */
	Flags2(0x10, 1, 0, 2),
	Transcipting(0x00, 1, 0, CDIR._DIR, Flags2),
	ForceFixedPitchPrint(0x01, 3, 0, CDIR._D_R, Flags2),
	ScrRedrawControl(0x02, 6, 0, CDIR._DI_, Flags2),//TODO also versions 7+8?
	PicsReq(0x03, 5, 0, CDIR.__IR, Flags2),
	UndoReq(0x04, 5, 0, CDIR.__IR, Flags2),
	MouseReq(0x05, 5, 0, CDIR.__IR, Flags2),
	ColorsReq(0x06, 5, 0, CDIR.____, Flags2),
	SoundFXReq(0x07, 5, 0, CDIR.__IR, Flags2),
	MenusReq(0x08, 6, 0, CDIR.__IR, Flags2),
	SerialCode(0x12, 2, 2, CDIR.C___, 6),
	SerialNum(0x12, 3, 0, CDIR.C___, 6),
	AbbrevTableLoc(0x18, 2, 0, CDIR.____, 2),
	FileLength(0x1A, 3, 0, CDIR.____, 2),
	FileChecksum(0x1C, 3, 0, CDIR.____, 2),
	InterpreterNum(0x1E, 4, 0, CDIR.__IR, 1),
	InterpreterVer(0x1F, 4, 0, CDIR.__IR, 1),
	ScrHeightLines(0x20, 4, 0, CDIR.__IR, 1),
	ScrWidthChars(0x21, 4, 0, CDIR.__IR, 1),
	ScrWidthUnits(0x22, 5, 0, CDIR.__IR, 2),
	ScrHeightUnits(0x24, 5, 0, CDIR.__IR, 2),
	FontWidthV5(0x26, 5, 5, CDIR.__IR, 1),
	FontHeightV6(0x26, 6, 0, CDIR.__IR, 1),//TODO also versions 7+8?
	FontHeightV5(0x27, 5, 5, CDIR.__IR, 1),
	FontWidthV6(0x27, 6, 0, CDIR.__IR, 1),//TODO also versions 7+8?
	RoutinesOff(0x28, 6, 7, CDIR.____, 2),
	StringsOff(0x2A, 6, 7, CDIR.____, 2),
	DefaultBGCol(0x2C, 5, 0, CDIR.__IR, 1),
	DefaultFGCol(0x2D, 5, 0, CDIR.__IR, 1),
	TermCharsTableLoc(0x2E, 5, 0, CDIR.____, 2),
	OutSt3WidthPixels(0x30, 6, 0, CDIR.__I_, 2),//TODO also versions 7+8?
	StandardRev(0x32, 1, 0, CDIR.__IR, 2),
	AlphabetTableLoc(0x34, 5, 0, CDIR.____, 2),
	HeaderExtTableLoc(0x36, 5, 0, CDIR.____, 2),
	PlayerName(0x38, 6, 0, CDIR.C___, 8),//TODO also versions 7+8?
	CompilerVer(0x3C, 1, 0, CDIR.C___, 4);

	/**
	 * For single bit fields, bit offset. Byte address in memory otherwise.
	 */
	public final int			addr;
	/**
	 * 0 for single bit fields
	 */
	public final int			len;
	/**
	 * null if not a single bit field
	 */
	public final HeaderField	bitfield;
	public final boolean		isBitfield;
	/**
	 * false for bitfields
	 */
	public final boolean		isCon, isDyn, isInt, isRst;
	public final int			minVersion, maxVersion;

	/**
	 * non-bitfield field
	 */
	private HeaderField(int addr, int minVersion, int maxVersion, CDIR cdir, int len)
	{
		this(addr, len, null, false, cdir, minVersion, maxVersion);
	}
	/**
	 * Bitfield
	 */
	private HeaderField(int addr, int minVersion, int maxVersion, int len)
	{
		this(addr, len, null, true, CDIR.____, minVersion, maxVersion);
	}
	/**
	 * Single bit field
	 */
	private HeaderField(int bitOff, int minVersion, int maxVersion, CDIR cdir, HeaderField bitfield)
	{
		this(bitOff, 0, bitfield, false, cdir, minVersion, maxVersion);
	}

	private HeaderField(int addr, int len, HeaderField bitfield, boolean isBitfield, CDIR cdir, int minVersion, int maxVersion)
	{
		this.addr = addr;
		this.len = len;
		this.bitfield = bitfield;
		this.isBitfield = isBitfield;
		this.isCon = cdir.isCon;
		this.isDyn = cdir.isDyn;
		this.isInt = cdir.isInt;
		this.isRst = cdir.isRst;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion < 1 ? -1 : maxVersion;
	}

	private static enum CDIR
	{
		____(false, false, false, false),
		___R(false, false, false, true),
		__I_(false, false, true, false),
		__IR(false, false, true, true),
		_D__(false, true, false, false),
		_D_R(false, true, false, true),
		_DI_(false, true, true, false),
		_DIR(false, true, true, true),
		C___(true, false, false, false),
		C__R(true, false, false, true),
		C_I_(true, false, true, false),
		C_IR(true, false, true, true),
		CD__(true, true, false, false),
		CD_R(true, true, false, true),
		CDI_(true, true, true, false),
		CDIR(true, true, true, true);
		public final boolean isCon, isDyn, isInt, isRst;
		private CDIR(boolean isCon, boolean isDyn, boolean isInt, boolean isRst)
		{
			this.isCon = isCon;
			this.isDyn = isDyn;
			this.isInt = isInt;
			this.isRst = isRst;
		}
	}
}