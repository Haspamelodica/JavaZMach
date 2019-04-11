package net.haspamelodica.javaz.core.io;

public class WindowPropsAttrs
{
	public static final int	LocYProp		= 0;
	public static final int	LocXProp		= 1;
	public static final int	SizeYProp		= 2;
	public static final int	SizeXProp		= 3;
	public static final int	CursorYProp		= 4;
	public static final int	CursorXProp		= 5;
	public static final int	MarginLProp		= 6;
	public static final int	MarginRProp		= 7;
	public static final int	NLIRoutineProp	= 8;
	public static final int	NLICountProp	= 9;
	public static final int	TextStyleProp	= 10;
	public static final int	ColorDataProp	= 11;
	public static final int	FontNumProp		= 12;
	public static final int	FontSizeProp	= 13;
	public static final int	AttrsProp		= 14;
	public static final int	LineCountProp	= 15;
	public static final int	TrueFGProp		= 16;
	public static final int	TrueBGProp		= 17;

	public static final int	WrappingAttr	= 0;
	public static final int	ScrollingAttr	= 1;
	public static final int	TranscriptAttr	= 2;
	public static final int	BufferedAttr	= 3;

	private final short[] properties;

	public WindowPropsAttrs()
	{
		properties = new short[18];
	}

	public int getProperty(int property)
	{
		return properties[property] & 0xFFFF;
	}
	public void setProperty(int property, int val)
	{
		properties[property] = (short) val;
	}

	/**
	 * 1 if set, 0 if clear
	 */
	public int getAttribute(int attribute)
	{
		return (getProperty(AttrsProp) >>> attribute) & 0b1;
	}
	public void setAttribute(int attribute, int val)
	{
		int oldAttrsProp = getProperty(AttrsProp);
		setProperty(AttrsProp, (oldAttrsProp & ~(1 << attribute)) | val << attribute);
	}
}