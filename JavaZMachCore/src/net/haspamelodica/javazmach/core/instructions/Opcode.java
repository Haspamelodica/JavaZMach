package net.haspamelodica.javazmach.core.instructions;

import static net.haspamelodica.javazmach.core.instructions.OpcodeKind.EXT;
import static net.haspamelodica.javazmach.core.instructions.OpcodeKind.OP0;
import static net.haspamelodica.javazmach.core.instructions.OpcodeKind.OP1;
import static net.haspamelodica.javazmach.core.instructions.OpcodeKind.OP2;
import static net.haspamelodica.javazmach.core.instructions.OpcodeKind.VAR;

public enum Opcode
{
	//TODO add min/max arg counts
	je/*             */(0x01, OP2, SBT._B_, 1, 1, 4, "je"),
	jl/*             */(0x02, OP2, SBT._B_, 1, "jl"),
	jg/*             */(0x03, OP2, SBT._B_, 1, "jg"),
	dec_chk/*        */(0x04, OP2, SBT._B_, 1, "dec_chk"),
	inc_chk/*        */(0x05, OP2, SBT._B_, 1, "inc_chk"),
	jin/*            */(0x06, OP2, SBT._B_, 1, "jin"),
	test/*           */(0x07, OP2, SBT._B_, 1, "test"),
	or/*             */(0x08, OP2, SBT.S__, 1, "or"),
	and/*            */(0x09, OP2, SBT.S__, 1, "and"),
	test_attr/*      */(0x0A, OP2, SBT._B_, 1, "test_attr"),
	set_attr/*       */(0x0B, OP2, SBT.___, 1, "set_attr"),
	clear_attr/*     */(0x0C, OP2, SBT.___, 1, "clear_attr"),
	store/*          */(0x0D, OP2, SBT.___, 1, "store"),
	insert_obj/*     */(0x0E, OP2, SBT.___, 1, "insert_obj"),
	loadw/*          */(0x0F, OP2, SBT.S__, 1, "loadw"),
	loadb/*          */(0x10, OP2, SBT.S__, 1, "loadb"),
	get_prop/*       */(0x11, OP2, SBT.S__, 1, "get_prop"),
	get_prop_addr/*  */(0x12, OP2, SBT.S__, 1, "get_prop_addr"),
	get_next_prop/*  */(0x13, OP2, SBT.S__, 1, "get_next_prop"),
	add/*            */(0x14, OP2, SBT.S__, 1, "add"),
	sub/*            */(0x15, OP2, SBT.S__, 1, "sub"),
	mul/*            */(0x16, OP2, SBT.S__, 1, "mul"),
	div/*            */(0x17, OP2, SBT.S__, 1, "div"),
	mod/*            */(0x18, OP2, SBT.S__, 1, "mod"),
	call_2s/*        */(0x19, OP2, SBT.S__, 4, "call_2s"),
	call_2n/*        */(0x1A, OP2, SBT.___, 5, "call_2n"),
	set_color/*      */(0x1B, OP2, SBT.___, 5, "set_color"),
	throw_/*         */(0x1C, OP2, SBT.___, 5, "throw"),

	jz/*             */(0x00, OP1, SBT._B_, 1, "jz"),
	get_sibling/*    */(0x01, OP1, SBT.SB_, 1, "get_sibling"),
	get_child/*      */(0x02, OP1, SBT.SB_, 1, "get_child"),
	get_parent/*     */(0x03, OP1, SBT.S__, 1, "get_parent"),
	get_prop_len/*   */(0x04, OP1, SBT.S__, 1, "get_prop_len"),
	inc/*            */(0x05, OP1, SBT.___, 1, "inc"),
	dec/*            */(0x06, OP1, SBT.___, 1, "dec"),
	print_addr/*     */(0x07, OP1, SBT.___, 1, "print_addr"),
	call_1s/*        */(0x08, OP1, SBT.S__, 4, "call_1s"),
	remove_obj/*     */(0x09, OP1, SBT.___, 1, "remove_obj"),
	print_obj/*      */(0x0A, OP1, SBT.___, 1, "print_obj"),
	ret/*            */(0x0B, OP1, SBT.___, 1, "ret"),
	jump/*           */(0x0C, OP1, SBT.___, 1, "jump"),//Not a branch instruction
	print_paddr/*    */(0x0D, OP1, SBT.___, 1, "print_paddr"),
	load/*           */(0x0E, OP1, SBT.S__, 1, "load"),
	not_V14/*        */(0x0F, OP1, SBT.S__, 1, 4, "not"),
	call_1n/*        */(0x0F, OP1, SBT.___, 5, "call_1n"),
	rtrue/*          */(0x00, OP0, SBT.___, 1, "rtrue"),
	rfalse/*         */(0x01, OP0, SBT.___, 1, "rfalse"),
	print/*          */(0x02, OP0, SBT.__T, 1, "print"),
	print_ret/*      */(0x03, OP0, SBT.__T, 1, "print_ret"),
	nop/*            */(0x04, OP0, SBT.___, 1, "nop"),
	save_V13/*       */(0x05, OP0, SBT._B_, 1, 3, "save"),
	save_V4/*        */(0x05, OP0, SBT.S__, 4, 4, "save"),//TODO: Is this a store or a branch opcode? Documentation contradicts itself
	restore_V13/*    */(0x06, OP0, SBT._B_, 1, 3, "restore"),
	restore_V4/*     */(0x06, OP0, SBT.S__, 4, 5, "restore"),//TODO: Is this a store or a branch opcode? Documentation contradicts itself
	restart/*        */(0x07, OP0, SBT.___, 1, "restart"),
	ret_popped/*     */(0x08, OP0, SBT.___, 1, "ret_popped"),
	pop/*            */(0x09, OP0, SBT.___, 1, 4, "pop"),
	catch_/*         */(0x09, OP0, SBT.S__, 5, "catch"),
	quit/*           */(0x0A, OP0, SBT.___, 1, "quit"),
	new_line/*       */(0x0B, OP0, SBT.___, 1, "new_line"),
	show_status/*    */(0x0C, OP0, SBT.___, 3, 3, "show_status"),
	verify/*         */(0x0D, OP0, SBT._B_, 3, "verify"),
	piracy/*         */(0x0F, OP0, SBT._B_, 5, "piracy"),

	call/*           */(0x00, VAR, SBT.S__, 1, 3, "call"),
	call_vs/*        */(0x00, VAR, SBT.S__, 4, "call_vs"),
	storew/*         */(0x01, VAR, SBT.___, 1, "storew"),
	storeb/*         */(0x02, VAR, SBT.___, 1, "storeb"),
	put_prop/*       */(0x03, VAR, SBT.___, 1, "put_prop"),
	sread/*          */(0x04, VAR, SBT.___, 1, 4, "sread"),
	aread/*          */(0x04, VAR, SBT.S__, 5, "aread"),
	print_char/*     */(0x05, VAR, SBT.___, 1, "print_char"),
	print_num/*      */(0x06, VAR, SBT.___, 1, "print_num"),
	random/*         */(0x07, VAR, SBT.S__, 1, "random"),
	push/*           */(0x08, VAR, SBT.___, 1, "push"),
	pull_V15/*       */(0x09, VAR, SBT.___, 1, 5, "pull"),
	pull_V6/*        */(0x09, VAR, SBT.S__, 6, 6, "pull"),
	pull_V7/*        */(0x09, VAR, SBT.___, 7, "pull"),
	split_window/*   */(0x0A, VAR, SBT.___, 3, "split_window"),
	set_window/*     */(0x0B, VAR, SBT.___, 3, "set_window"),
	call_vs2/*       */(0x0C, VAR, SBT.S__, 4, true, "call_vs2"),
	erase_window/*   */(0x0D, VAR, SBT.___, 4, "erase_window"),
	erase_line/*     */(0x0E, VAR, SBT.___, 4, "erase_line"),
	set_cursor/*     */(0x0F, VAR, SBT.___, 4, "set_cursor"),
	get_cursor/*     */(0x10, VAR, SBT.___, 4, "get_cursor"),
	set_text_style/* */(0x11, VAR, SBT.___, 4, "set_text_style"),
	buffer_mode/*    */(0x12, VAR, SBT.___, 4, "buffer_mode"),
	output_stream/*  */(0x13, VAR, SBT.___, 3, "output_stream"),
	input_stream/*   */(0x14, VAR, SBT.___, 3, "input_stream"),
	sound_effect/*   */(0x15, VAR, SBT.___, 5, "sound_effect"),
	read_char/*      */(0x16, VAR, SBT.S__, 4, "read_char"),
	scan_table/*     */(0x17, VAR, SBT.SB_, 4, "scan_table"),//TODO is this correct? where is the branch target specified? See documentation...
	not_V5/*         */(0x18, VAR, SBT.S__, 5, "not"),
	call_vn/*        */(0x19, VAR, SBT.___, 5, "call_vn"),
	call_vn2/*       */(0x1A, VAR, SBT.___, 5, true, "call_vn2"),
	tokenise/*       */(0x1B, VAR, SBT.___, 5, "tokenise"),
	encode_text/*    */(0x1C, VAR, SBT.___, 5, "encode_text"),
	copy_table/*     */(0x1D, VAR, SBT.___, 5, "copy_table"),
	print_table/*    */(0x1E, VAR, SBT.___, 5, "print_table"),
	check_arg_count/**/(0x1F, VAR, SBT._B_, 5, "check_arg_count"),//TODO is this correct? where is the branch target specified? See documentation...

	save_V5/*        */(0x00, EXT, SBT.S__, 5, "save"),
	restore_V5/*     */(0x01, EXT, SBT.S__, 5, "restore"),
	log_shift/*      */(0x02, EXT, SBT.S__, 5, "log_shift"),
	art_shift/*      */(0x03, EXT, SBT.S__, 5, "art_shift"),
	set_font/*       */(0x04, EXT, SBT.S__, 5, "set_font"),
	draw_picture/*   */(0x05, EXT, SBT.___, 6, "draw_picture"),
	picture_data/*   */(0x06, EXT, SBT._B_, 6, "picture_data"),
	erase_picture/*  */(0x07, EXT, SBT.___, 6, "erase_picture"),
	set_margins/*    */(0x08, EXT, SBT.___, 6, "set_margins"),
	save_undo/*      */(0x09, EXT, SBT.S__, 5, "save_undo"),
	restore_undo/*   */(0x0A, EXT, SBT.S__, 5, "restore_undo"),
	print_unicode/*  */(0x0B, EXT, SBT.___, 5, "print_unicode"),
	check_unicode/*  */(0x0C, EXT, SBT.S__, 5, "check_unicode"),
	set_true_color/* */(0x0D, EXT, SBT.___, 5, "set_true_color"),
	move_window/*    */(0x10, EXT, SBT.___, 6, "move_window"),
	window_size/*    */(0x11, EXT, SBT.___, 6, "window_size"),
	window_style/*   */(0x12, EXT, SBT.___, 6, "window_style"),
	get_wind_prop/*  */(0x13, EXT, SBT.S__, 6, "get_wind_prop"),
	scroll_window/*  */(0x14, EXT, SBT.___, 6, "scroll_window"),
	pop_stack/*      */(0x15, EXT, SBT.___, 6, "pop_stack"),
	read_mouse/*     */(0x16, EXT, SBT.___, 6, "read_mouse"),
	mouse_window/*   */(0x17, EXT, SBT.___, 6, "mouse_window"),
	push_stack/*     */(0x18, EXT, SBT._B_, 6, "push_stack"),
	put_wind_prop/*  */(0x19, EXT, SBT.___, 6, "put_wind_prop"),
	print_form/*     */(0x1A, EXT, SBT.___, 6, "print_form"),
	make_menu/*      */(0x1B, EXT, SBT._B_, 6, "make_menu"),
	picture_table/*  */(0x1C, EXT, SBT.___, 6, "picture_table"),
	buffer_screen/*  */(0x1D, EXT, SBT.S__, 6, "buffer_screen"),

	_unknown_instr/* */(0xFF, null, SBT.___, -1, -1, -1, -1, "<unknown>");

	public final int		opcodeNumber;
	/**
	 * null means this opcode is in EXTENDED "range".
	 */
	public final OpcodeKind	range;
	public final boolean	hasTwoOperandTypeBytes;
	public final int		minArgs, maxArgs;
	public final int		minVersion, maxVersion;
	public final boolean	isStoreOpcode;
	public final boolean	isBranchOpcode;
	public final boolean	isTextOpcode;
	public final String		name;

	private Opcode(int opcodeNumber, OpcodeKind range, SBT sbt, int minVersion, String name)
	{
		this(opcodeNumber, range, sbt, minVersion, -1, name);
	}
	private Opcode(int opcodeNumber, OpcodeKind range, SBT sbt, int minVersion, int minArgs, int maxArgs, String name)
	{
		this(opcodeNumber, range, sbt, minVersion, -1, minArgs, maxArgs, name);
	}
	private Opcode(int opcodeNumber, OpcodeKind range, SBT sbt, int minVersion, int maxVersion, String name)
	{
		this(opcodeNumber, range, sbt, minVersion, maxVersion, false, name);
	}
	private Opcode(int opcodeNumber, OpcodeKind range, SBT sbt, int minVersion, int maxVersion, int minArgs, int maxArgs, String name)
	{
		this(opcodeNumber, range, sbt, minVersion, maxVersion, minArgs, maxArgs, false, name);
	}
	private Opcode(int opcodeNumber, OpcodeKind range, SBT sbt, int minVersion, boolean hasTwoOperandTypeBytes, String name)
	{
		this(opcodeNumber, range, sbt, minVersion, -1, hasTwoOperandTypeBytes, name);
	}
	private Opcode(int opcodeNumber, OpcodeKind range, SBT sbt, int minVersion, int minArgs, int maxArgs, boolean hasTwoOperandTypeBytes, String name)
	{
		this(opcodeNumber, range, sbt, minVersion, -1, minArgs, maxArgs, hasTwoOperandTypeBytes, name);
	}
	private Opcode(int opcodeNumber, OpcodeKind range, SBT sbt, int minVersion, int maxVersion, boolean hasTwoOperandTypeBytes, String name)
	{
		this(opcodeNumber, range, sbt, minVersion, maxVersion, defaultMinArgs(range), defaultMaxArgs(range, hasTwoOperandTypeBytes), hasTwoOperandTypeBytes, name);
	}
	private static int defaultMinArgs(OpcodeKind range)
	{
		if(range == null)
			return -1;
		switch(range)
		{
			case OP0:
				return 0;
			case OP1:
				return 1;
			case OP2:
				return 2;
			case VAR:
			case EXT:
				return 0;
			default:
				throw new IllegalArgumentException("Unknown enum type: " + range);
		}
	}
	private static int defaultMaxArgs(OpcodeKind range, boolean hasTwoOperandTypeBytes)
	{
		if(range == null)
			return -1;
		switch(range)
		{
			case OP0:
				return 0;
			case OP1:
				return 1;
			case OP2:
				return 2;
			case VAR:
				return hasTwoOperandTypeBytes ? 8 : 4;
			case EXT:
				return 4;
			default:
				throw new IllegalArgumentException("Unknown enum type: " + range);
		}
	}
	private Opcode(int opcodeNumber, OpcodeKind range, SBT sbt, int minVersion, int maxVersion, int minArgs, int maxArgs, boolean hasTwoOperandTypeBytes, String name)
	{
		this.opcodeNumber = opcodeNumber;
		this.range = range;
		this.hasTwoOperandTypeBytes = hasTwoOperandTypeBytes;
		this.minArgs = minArgs;
		this.maxArgs = maxArgs;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
		this.isStoreOpcode = sbt.isStore;
		this.isBranchOpcode = sbt.isBranch;
		this.isTextOpcode = sbt.isText;
		this.name = name;
	}

	public static Opcode decode(int opcodeByte, OpcodeForm form, OpcodeKind kind, int version)
	{
		//TODO make this faster.
		int opcodeNumber;
		switch(form)
		{
			case LONG:
			case VARIABLE:
				opcodeNumber = opcodeByte & 0x1F;//bits 4-0
				break;
			case SHORT:
				opcodeNumber = opcodeByte & 0x0F;//bits 3-0
				break;
			case EXTENDED:
				throw new IllegalArgumentException("You should call decodeExtended() for extended opcodes!");
			default:
				throw new IllegalArgumentException("Unknown enum type: " + form);
		}
		for(Opcode op : values())
			if(op.range != null
					&& op.opcodeNumber == opcodeNumber && op.range == kind
					&& op.minVersion <= version && (op.maxVersion <= 0 || op.maxVersion >= version))
				return op;
		return _unknown_instr;
	}
	public static Opcode decodeExtended(int secondOpcodeByte, int version)
	{
		//TODO make this faster.
		for(Opcode op : values())
			if(op.range != null
					&& op.opcodeNumber == secondOpcodeByte && op.range == EXT
					&& op.minVersion <= version && (op.maxVersion <= 0 || op.maxVersion >= version))
				return op;
		return _unknown_instr;
	}

	private static enum SBT
	{
		___(false, false, false),
		S__(true, false, false),
		_B_(false, true, false),
		SB_(true, true, false),
		__T(false, false, true),
		S_T(true, false, true),
		_BT(false, true, true),
		SBT(true, true, true);

		public final boolean	isStore;
		public final boolean	isBranch;
		public final boolean	isText;

		private SBT(boolean isStore, boolean isBranch, boolean isText)
		{
			this.isStore = isStore;
			this.isBranch = isBranch;
			this.isText = isText;
		}
	}
}