package net.haspamelodica.javaz;

import static net.haspamelodica.javaz.OperandCount.*;

public enum Opcode
{
	je(0x1, OP2, 1, 0, 1, 0, "je"),
	jl(0x2, OP2, 1, 0, 1, 0, "jl"),
	jg(0x3, OP2, 1, 0, 1, 0, "jg"),
	dec_chk(0x4, OP2, 1, 0, 1, 0, "dec_chk"),
	inc_chk(0x5, OP2, 1, 0, 1, 0, "inc_chk"),
	jin(0x6, OP2, 1, 0, 1, 0, "jin"),
	test(0x7, OP2, 1, 0, 1, 0, "test"),
	or(0x8, OP2, 1, 1, 0, 0, "or"),
	and(0x9, OP2, 1, 1, 0, 0, "and"),
	test_attr(0xA, OP2, 1, 0, 1, 0, "test_attr"),
	set_attr(0xB, OP2, 1, 0, 0, 0, "set_attr"),
	clear_attr(0xC, OP2, 1, 0, 0, 0, "clear_attr"),
	store(0xD, OP2, 1, 0, 0, 0, "store"),
	insert_obj(0xE, OP2, 1, 0, 0, 0, "insert_obj"),
	loadw(0xF, OP2, 1, 1, 0, 0, "loadw"),
	loadb(0x10, OP2, 1, 1, 0, 0, "loadb"),
	get_prop(0x11, OP2, 1, 1, 0, 0, "get_prop"),
	get_prop_addr(0x12, OP2, 1, 1, 0, 0, "get_prop_addr"),
	get_next_prop(0x13, OP2, 1, 1, 0, 0, "get_next_prop"),
	add(0x14, OP2, 1, 1, 0, 0, "add"),
	sub(0x15, OP2, 1, 1, 0, 0, "sub"),
	mul(0x16, OP2, 1, 1, 0, 0, "mul"),
	div(0x17, OP2, 1, 1, 0, 0, "div"),
	mod(0x18, OP2, 1, 1, 0, 0, "mod"),
	call_2s(0x19, OP2, 4, 1, 0, 0, "call_2s"),
	call_2n(0x1A, OP2, 5, 0, 0, 0, "call_2n"),
	set_colour(0x1B, OP2, 5, 0, 0, 0, "set_colour"),
	throw_(0x1C, OP2, 5, 0, 0, 0, "throw"),
	jz(0x0, OP1, 1, 0, 1, 0, "jz"),
	get_sibling(0x1, OP1, 1, 1, 1, 0, "get_sibling"),
	get_child(0x2, OP1, 1, 1, 1, 0, "get_child"),
	get_parent(0x3, OP1, 1, 1, 0, 0, "get_parent"),
	get_prop_len(0x4, OP1, 1, 1, 0, 0, "get_prop_len"),
	inc(0x5, OP1, 1, 0, 0, 0, "inc"),
	dec(0x6, OP1, 1, 0, 0, 0, "dec"),
	print_addr(0x7, OP1, 1, 0, 0, 0, "print_addr"),
	call_1s(0x8, OP1, 4, 1, 0, 0, "call_1s"),
	remove_obj(0x9, OP1, 1, 0, 0, 0, "remove_obj"),
	print_obj(0xA, OP1, 1, 0, 0, 0, "print_obj"),
	ret(0xB, OP1, 1, 0, 0, 0, "ret"),
	jump(0xC, OP1, 1, 0, 0, 0, "jump"),//Not a branch instruction
	print_paddr(0xD, OP1, 1, 0, 0, 0, "print_paddr"),
	load(0xE, OP1, 1, 1, 0, 0, "load"),
	not_V14(0xF, OP1, 1, 4, 1, 0, 0, "not"),
	call_1n(0xF, OP1, 5, 0, 0, 0, "call_1n"),
	rtrue(0x0, OP0, 1, 0, 0, 0, "rtrue"),
	rfalse(0x1, OP0, 1, 0, 0, 0, "rfalse"),
	print(0x2, OP0, 1, 0, 0, 0, "print"),
	print_ret(0x3, OP0, 1, 0, 0, 0, "print_ret"),
	nop(0x4, OP0, 1, 0, 0, 0, "nop"),
	save_V13(0x5, OP0, 1, 3, 0, 1, 0, "save"),
	save_V4(0x5, OP0, 4, 4, 1, 0, 0, "save"),//TODO: Is this a store or a branch opcode? Documentation contradicts itself
	restore_V13(0x6, OP0, 1, 3, 0, 1, 0, "restore"),
	restore_V4(0x6, OP0, 4, 5, 1, 0, 0, "restore"),//TODO: Is this a store or a branch opcode? Documentation contradicts itself
	restart(0x7, OP0, 1, 0, 0, 0, "restart"),
	ret_popped(0x8, OP0, 1, 0, 0, 0, "ret_popped"),
	pop(0x9, OP0, 1, 4, 0, 0, 0, "pop"),
	catch_(0x9, OP0, 5, 1, 0, 0, "catch"),
	quit(0xA, OP0, 1, 0, 0, 0, "quit"),
	new_line(0xB, OP0, 1, 0, 0, 0, "new_line"),
	show_status(0xC, OP0, 3, 3, 0, 0, 0, "show_status"),
	verify(0xD, OP0, 3, 0, 1, 0, "verify"),
	piracy(0xF, OP0, 5, 0, 1, 0, "piracy"),
	call(0x0, VAR, 1, 3, 1, 0, 0, "call"),
	call_vs(0x0, VAR, 4, 1, 0, 0, "call_vs"),
	storew(0x1, VAR, 1, 0, 0, 0, "storew"),
	storeb(0x2, VAR, 1, 0, 0, 0, "storeb"),
	put_prop(0x3, VAR, 1, 0, 0, 0, "put_prop"),
	sread(0x4, VAR, 1, 4, 0, 0, 0, "sread"),
	aread(0x4, VAR, 5, 1, 0, 0, "aread"),
	print_char(0x5, VAR, 1, 0, 0, 0, "print_char"),
	print_num(0x6, VAR, 1, 0, 0, 0, "print_num"),
	random(0x7, VAR, 1, 1, 0, 0, "random"),
	push(0x8, VAR, 1, 0, 0, 0, "push"),
	pull_V15(0x9, VAR, 1, 5, 0, 0, 0, "pull"),
	pull_V6(0x9, VAR, 6, 1, 0, 0, "pull"),
	split_window(0xA, VAR, 3, 0, 0, 0, "split_window"),
	set_window(0xB, VAR, 3, 0, 0, 0, "set_window"),
	call_vs2(0xC, VAR, 4, 1, 0, 0, "call_vs2"),
	erase_window(0xD, VAR, 4, 0, 0, 0, "erase_window"),
	erase_line(0xE, VAR, 4, 0, 0, 0, "erase_line"),
	set_cursor(0xF, VAR, 4, 0, 0, 0, "set_cursor"),
	get_cursor(0x10, VAR, 4, 0, 0, 0, "get_cursor"),
	set_text_style(0x11, VAR, 4, 0, 0, 0, "set_text_style"),
	buffer_mode(0x12, VAR, 4, 0, 0, 0, "buffer_mode"),
	output_stream(0x13, VAR, 3, 0, 0, 0, "output_stream"),
	input_stream(0x14, VAR, 3, 0, 0, 0, "input_stream"),
	sound_effect(0x15, VAR, 5, 0, 0, 0, "sound_effect"),
	read_char(0x16, VAR, 4, 1, 0, 0, "read_char"),
	scan_table(0x17, VAR, 4, 1, 1, 0, "scan_table"),//TODO is this correct? where is the branch target specified? See documentation...
	not_V5(0x18, VAR, 5, 1, 0, 0, "not"),
	call_vn(0x19, VAR, 5, 0, 0, 0, "call_vn"),
	call_vn2(0x1A, VAR, 5, 0, 0, 0, "call_vn2"),
	tokenise(0x1B, VAR, 5, 0, 0, 0, "tokenise"),
	encode_text(0x1C, VAR, 5, 0, 0, 0, "encode_text"),
	copy_table(0x1D, VAR, 5, 0, 0, 0, "copy_table"),
	print_table(0x1E, VAR, 5, 0, 0, 0, "print_table"),
	check_arg_count(0x1F, VAR, 5, 0, 1, 0, "check_arg_count"),//TODO is this correct? where is the branch target specified? See documentation...
	save_V5(0x0, null, 5, 1, 0, 0, "save"),
	restore_V5(0x1, null, 5, 1, 0, 0, "restore"),
	log_shift(0x2, null, 5, 1, 0, 0, "log_shift"),
	art_shift(0x3, null, 5, 1, 0, 0, "art_shift"),
	set_font(0x4, null, 5, 1, 0, 0, "set_font"),
	draw_picture(0x5, null, 6, 0, 0, 0, "draw_picture"),
	picture_data(0x6, null, 6, 0, 1, 0, "picture_data"),
	erase_picture(0x7, null, 6, 0, 0, 0, "erase_picture"),
	set_margins(0x8, null, 6, 0, 0, 0, "set_margins"),
	save_undo(0x9, null, 5, 1, 0, 0, "save_undo"),
	restore_undo(0xA, null, 5, 1, 0, 0, "restore_undo"),
	print_unicode(0xB, null, 5, 0, 0, 0, "print_unicode"),
	check_unicode(0xC, null, 5, 1, 0, 0, "check_unicode"),
	set_true_colour(0xD, null, 5, 0, 0, 0, "set_true_colour"),
	move_window(0x10, null, 6, 0, 0, 0, "move_window"),
	window_size(0x11, null, 6, 0, 0, 0, "window_size"),
	window_style(0x12, null, 6, 0, 0, 0, "window_style"),
	get_wind_prop(0x13, null, 6, 1, 0, 0, "get_wind_prop"),
	scroll_window(0x14, null, 6, 0, 0, 0, "scroll_window"),
	pop_stack(0x15, null, 6, 0, 0, 0, "pop_stack"),
	read_mouse(0x16, null, 6, 0, 0, 0, "read_mouse"),
	mouse_window(0x17, null, 6, 0, 0, 0, "mouse_window"),
	push_stack(0x18, null, 6, 0, 1, 0, "push_stack"),
	put_wind_prop(0x19, null, 6, 0, 0, 0, "put_wind_prop"),
	print_form(0x1A, null, 6, 0, 0, 0, "print_form"),
	make_menu(0x1B, null, 6, 0, 1, 0, "make_menu"),
	picture_table(0x1C, null, 6, 0, 0, 0, "picture_table"),
	buffer_screen(0x1D, null, 6, 1, 0, 0, "buffer_screen"),

	_unknown_instr(-1, null, -1, -1, 0, 0, 0, "<unknown>");

	public final int			opcodeNumber;
	/**
	 * null means this opcode is in EXTENDED "range".
	 */
	public final OperandCount	count;
	public final boolean		hasTwoOperandTypeBytes;
	public final int			minVersion, maxVersion;
	public final boolean		isStoreOpcode;
	public final boolean		isBranchOpcode;
	public final boolean		isTextOpcode;
	public final String			name;

	private Opcode(int opcodeNumber, OperandCount count, int store, int branch, int text, String name)
	{
		this(opcodeNumber, count, 1, store, branch, text, name);
	}
	private Opcode(int opcodeNumber, OperandCount count, int minVersion, int store, int branch, int text, String name)
	{
		this(opcodeNumber, count, minVersion, -1, store, branch, text, name);
	}
	private Opcode(int opcodeNumber, OperandCount count, int minVersion, int maxVersion, int store, int branch, int text, String name)
	{
		this(opcodeNumber, count, false, minVersion, maxVersion, store, branch, text, name);
	}
	private Opcode(int opcodeNumber, OperandCount count, boolean hasTwoOperandTypeBytes, int minVersion, int store, int branch, int text, String name)
	{
		this(opcodeNumber, count, hasTwoOperandTypeBytes, minVersion, -1, store, branch, text, name);
	}
	private Opcode(int opcodeNumber, OperandCount count, boolean hasTwoOperandTypeBytes, int minVersion, int maxVersion, int store, int branch, int text, String name)
	{
		this.opcodeNumber = opcodeNumber;
		this.count = count;
		this.hasTwoOperandTypeBytes = hasTwoOperandTypeBytes;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
		this.isStoreOpcode = store != 0;
		this.isBranchOpcode = branch != 0;
		this.isTextOpcode = text != 0;
		this.name = name;
	}

	public static Opcode decode(int opcodeByte, OpcodeForm form, OperandCount count, int version)
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
			if(op.opcodeNumber == opcodeNumber && op.count == count && op.minVersion <= version && (op.maxVersion <= 0 || op.maxVersion >= version))
				return op;
		return _unknown_instr;
	}

	public static Opcode decodeExtended(int secondOpcodeByte, int version)
	{
		//TODO make this faster.
		for(Opcode op : values())
			if(op.opcodeNumber == secondOpcodeByte && op.count == null && op.minVersion <= version && (op.maxVersion <= 0 || op.maxVersion >= version))
				return op;
		return _unknown_instr;
	}
}