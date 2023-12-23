package net.haspamelodica.javazmach.core.io;

import static net.haspamelodica.javazmach.core.header.HeaderField.DefaultBGCol;
import static net.haspamelodica.javazmach.core.header.HeaderField.DefaultFGCol;
import static net.haspamelodica.javazmach.core.header.HeaderField.FontHeightV5;
import static net.haspamelodica.javazmach.core.header.HeaderField.FontHeightV6;
import static net.haspamelodica.javazmach.core.header.HeaderField.FontWidthV5;
import static net.haspamelodica.javazmach.core.header.HeaderField.FontWidthV6;
import static net.haspamelodica.javazmach.core.header.HeaderField.ScrHeightLines;
import static net.haspamelodica.javazmach.core.header.HeaderField.ScrHeightUnits;
import static net.haspamelodica.javazmach.core.header.HeaderField.ScrWidthChars;
import static net.haspamelodica.javazmach.core.header.HeaderField.ScrWidthUnits;
import static net.haspamelodica.javazmach.core.header.HeaderField.StatLineType;
import static net.haspamelodica.javazmach.core.header.HeaderField.TermCharsTableLoc;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.AttrsProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.BufferedAttr;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.ColorDataProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.CursorXProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.CursorYProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.FontNumProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.FontSizeProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.LineCountProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.LocXProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.LocYProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.MarginLProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.MarginRProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.NLICountProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.NLIRoutineProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.ScrollingAttr;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.SizeXProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.SizeYProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.TextStyleProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.TranscriptAttr;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.TrueBGProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.TrueFGProp;
import static net.haspamelodica.javazmach.core.io.WindowPropsAttrs.WrappingAttr;

import java.util.Arrays;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.ReadOnlyMemory;
import net.haspamelodica.javazmach.core.memory.WritableUndoableBuffer;
import net.haspamelodica.javazmach.core.memory.ZeroTerminatedReadOnlyByteSet;
import net.haspamelodica.javazmach.core.text.ZSCIICharStream;

public class IOCard
{
	private static final int OUTPUT_BUFFER_OVERHEAD = 20;

	private final int version;

	private final boolean replaceOnlyFirstSpaceWithExtraNL;

	private final HeaderParser			headerParser;
	private final VideoCard				videoCard;
	private final WindowPropsAttrs[]	windowProperties;

	private boolean								isTimeGame;
	private int									defaultTrueBG;
	private final ZeroTerminatedReadOnlyByteSet	terminatingZSCIIChars;

	private int		unitsYRemainingToNextMORE;
	private boolean	extraNL;
	private int		firstNonSpaceIndex;

	private int[]	outputBufferChars;
	private int[]	outputBufferFonts;
	private int[]	outputBufferStyles;
	private int[]	outputBufferFGs;
	private int[]	outputBufferBGs;
	private int[]	outputBufferWidths;
	private int		outputBufferLength;
	private int		outputBufferWidth;

	private WindowPropsAttrs currentWindowProperties;

	public IOCard(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, VideoCard videoCard)
	{
		this.version = version;

		this.replaceOnlyFirstSpaceWithExtraNL = config.getBool("io.wrapping.replace_only_first_space");

		this.headerParser = headerParser;
		this.videoCard = videoCard;
		int windowCount = version < 3 ? 1 : version == 6 ? 8 : 2;
		this.windowProperties = new WindowPropsAttrs[windowCount];
		for(int w = 0; w < windowCount; w ++)
			windowProperties[w] = new WindowPropsAttrs();

		this.outputBufferChars = new int[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferFonts = new int[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferStyles = new int[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferFGs = new int[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferBGs = new int[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferWidths = new int[OUTPUT_BUFFER_OVERHEAD];

		this.terminatingZSCIIChars = new ZeroTerminatedReadOnlyByteSet(mem);
	}

	public void reset()
	{
		isTimeGame = version == 3 && headerParser.getField(StatLineType) == 1;
		extraNL = false;
		outputBufferLength = 0;
		firstNonSpaceIndex = -1;
		int scrWidth = videoCard.getScreenWidth();
		int scrHeight = videoCard.getScreenHeight();
		unitsYRemainingToNextMORE = scrHeight - videoCard.getFontHeight(1);//MORE takes one line
		int defaultTrueFG = videoCard.getDefaultTrueFG();
		defaultTrueBG = videoCard.getDefaultTrueBG();
		int defaultFG = trueColToCol(defaultTrueFG);
		int defaultBG = trueColToCol(defaultTrueBG);
		int colorData = defaultFG + (defaultBG << 8);
		int fontWidth = videoCard.getCharWidth('0', 1, 0);//use '0' per definition
		int fontHeight = videoCard.getFontHeight(1);
		int fontSize = fontWidth + (fontHeight << 8);
		for(int w = 0; w < windowProperties.length; w ++)
		{
			WindowPropsAttrs p = windowProperties[w];
			p.setProperty(LocYProp, 1);
			p.setProperty(LocXProp, 1);
			p.setProperty(SizeYProp, w < 2 ? scrHeight : 0);
			p.setProperty(SizeXProp, w < 1 ? scrWidth : 0);
			p.setProperty(CursorYProp, scrHeight - fontHeight);
			p.setProperty(CursorXProp, 1);
			p.setProperty(MarginLProp, 0);
			p.setProperty(MarginRProp, 0);
			p.setProperty(NLIRoutineProp, 0);
			p.setProperty(NLICountProp, 0);
			p.setProperty(TextStyleProp, 0);
			p.setProperty(ColorDataProp, colorData);
			p.setProperty(FontNumProp, 1);
			p.setProperty(FontSizeProp, fontSize);
			p.setProperty(AttrsProp, 0);
			p.setProperty(LineCountProp, 0);
			p.setProperty(TrueFGProp, defaultTrueFG);
			p.setProperty(TrueBGProp, defaultTrueBG);

			p.setAttribute(WrappingAttr, w < 1 ? 1 : 0);
			p.setAttribute(ScrollingAttr, w < 1 ? 1 : 0);
			p.setAttribute(TranscriptAttr, w < 1 ? 1 : 0);
			//Window 1 in V5 is the only combination where buffering is off by default
			p.setAttribute(BufferedAttr, version == 5 && w == 1 ? 0 : 1);
		}
		if(version > 3)
		{
			headerParser.setField(ScrWidthChars, scrWidth / fontWidth, 2);
			headerParser.setField(ScrHeightLines, scrHeight / fontHeight, 2);
		}
		if(version > 4)
		{
			headerParser.setField(ScrWidthUnits, scrWidth, 2);
			headerParser.setField(ScrHeightUnits, scrHeight, 2);
			//write these or read from these? Section 8 and zmach06e.pdf contradict
			headerParser.setField(DefaultFGCol, scrWidth, 2);
			headerParser.setField(DefaultBGCol, scrHeight, 2);
			if(version == 5)
			{
				headerParser.setField(FontWidthV5, fontWidth, 2);
				headerParser.setField(FontHeightV5, fontHeight, 2);
			} else
			{
				headerParser.setField(FontWidthV6, fontWidth, 2);
				headerParser.setField(FontHeightV6, fontHeight, 2);
			}
		}
		videoCard.eraseArea(0, 0, scrWidth, scrHeight, defaultTrueBG);
		videoCard.flushScreen();
		currentWindowProperties = windowProperties[0];
		int terminatingCharsTableLoc = version < 5 ? 0 : headerParser.getField(TermCharsTableLoc);
		terminatingZSCIIChars.setStartAddr(terminatingCharsTableLoc == 0 ? -1 : terminatingCharsTableLoc);
	}
	private int trueColToCol(int trueCol)
	{
		int bestFittingCol = -1;
		int bestFittingDistSqr = Integer.MAX_VALUE;
		for(int c = 2; c < 13; c ++)
		{
			int currentTrueCol = videoCard.getTrueColor(c);
			int distR = ((currentTrueCol & 0x00_1F) >>> 0x0) - ((trueCol & 0x00_1F) >>> 0x0);
			int distG = ((currentTrueCol & 0x03_E0) >>> 0x5) - ((trueCol & 0x03_E0) >>> 0x5);
			int distB = ((currentTrueCol & 0x7C_00) >>> 0xA) - ((trueCol & 0x7C_00) >>> 0xA);
			int distSqr = distR * distR + distG * distG + distB * distB;
			if(distSqr < bestFittingDistSqr)
			{
				bestFittingCol = c;
				bestFittingDistSqr = distSqr;
			}
		}
		return bestFittingCol;
	}
	public void printZSCII(int zsciiChar)
	{
		boolean isSpace = zsciiChar == 9 || zsciiChar == 11 || zsciiChar == 32;
		if(zsciiChar == 13)
		{
			flushBuffer();
			extraNL = false;
			newlineMoveCursor();
		} else
		{
			WindowPropsAttrs properties = currentWindowProperties;
			//"If in V6 character 9 is to be output, and the cursor is to the right of the left margin, or ExtraNL
			//is true, then character 32 is used instead." - zmach06e.pdf, page 28
			if(zsciiChar == 9 && (extraNL || properties.getProperty(CursorXProp) > properties.getProperty(MarginLProp)))
				zsciiChar = 32;
			if(firstNonSpaceIndex >= 0 && isSpace)
				flushBuffer();
			if(zsciiChar != 0)//ZSCII 0 is "no char"
				appendToBuffer(zsciiChar, isSpace);
			if(properties.getAttribute(BufferedAttr) == 0)
				flushBuffer();
		}
	}
	public void selectWindow(int window)
	{
		currentWindowProperties = windowProperties[window];
	}
	public void eraseWindow(int window)
	{
		if(window == -1 || window == -2)
		{
			videoCard.eraseArea(0, 0, videoCard.getScreenWidth(), videoCard.getScreenHeight(), defaultTrueBG);
			if(window == -1)
				splitScreen(0);
		}
	}
	public void setBufferMode(int window, int bufferMode)
	{
		setBufferMode(windowProperties[window], bufferMode);
	}
	public void setBufferMode(int bufferMode)
	{
		setBufferMode(currentWindowProperties, bufferMode);
	}
	private void setBufferMode(WindowPropsAttrs props, int bufferMode)
	{
		props.setAttribute(BufferedAttr, bufferMode);
	}
	public void splitScreen(int w1Height)
	{
		int width = videoCard.getScreenWidth();
		int screenHeight = videoCard.getScreenHeight();
		if(w1Height > screenHeight)
			w1Height = screenHeight;
		int w0Height = screenHeight - w1Height;
		setProperty(0, LocXProp, 1);
		setProperty(1, LocXProp, 1);
		setProperty(0, LocYProp, 1);
		setProperty(1, LocYProp, 1);
		setProperty(0, SizeXProp, width);
		setProperty(1, SizeXProp, width);
		setProperty(0, SizeYProp, w0Height);
		setProperty(1, SizeYProp, w1Height);
	}
	public int getProperty(int window, int property)
	{
		return windowProperties[window].getProperty(property);
	}
	public int getPropertyCurrentWindow(int property)
	{
		return currentWindowProperties.getProperty(property);
	}
	public void setPropertyCurrentWindow(int property, int val)
	{
		setProperty(currentWindowProperties, property, val);
	}
	public void setProperty(int window, int property, int val)
	{
		setProperty(windowProperties[window], property, val);
	}
	private void setProperty(WindowPropsAttrs properties, int property, int val)
	{
		properties.setProperty(property, val);
		//TODO update other properties, header fields...
	}
	public void showStatusBar(ZSCIICharStream zsciiChars, int scoreOrHours, int turnsOrMinutes)
	{
		videoCard.showStatusBar(zsciiChars, scoreOrHours, turnsOrMinutes, isTimeGame);
	}
	/**
	 * Stores ZSCII chars as bytes in <code>targetTextBuffer</code> from the current input stream.
	 * Input runs until <code>maxZSCIIChars</code> ZSCII chars have been read,
	 * or ZSCII 13 or one of <code>terminatingZSCIIChars</code> has been read.
	 * The terminating ZSCII character (if any) is not stored.
	 * Returns the terminating ZSCII character,
	 * or -1 if <code>maxZSCIIChars</code> have been read,
	 * or -2 if the end of input has been reached.
	 */
	public int inputToTextBuffer(WritableUndoableBuffer targetTextBuffer)
	{
		flushBufferAndScreen();
		videoCard.hintInputCharUsage(InputUsageHint.COMMAND_START);
		int terminatingZSCIIChar = -1;
		while(!targetTextBuffer.isFull())
		{
			int zsciiChar = videoCard.nextInputChar();
			if(zsciiChar == 8)
			{
				if(!targetTextBuffer.isEmpty())
				{
					zsciiChar = targetTextBuffer.readLastEntryByte(0);
					int font = currentWindowProperties.getProperty(FontNumProp);
					int style = currentWindowProperties.getProperty(TextStyleProp);
					int width = videoCard.getCharWidth(zsciiChar, font, style);
					int newCursorX = currentWindowProperties.getProperty(CursorXProp) - width;
					if(newCursorX > 0)
					{
						targetTextBuffer.undoLastEntry();
						currentWindowProperties.setProperty(CursorXProp, newCursorX);
						int cursorY = currentWindowProperties.getProperty(CursorYProp);
						int col = currentWindowProperties.getProperty(ColorDataProp);
						int trueBG = videoCard.getTrueColor(col >>> 8);
						videoCard.eraseArea(newCursorX - 1, cursorY - 1, width, videoCard.getFontHeight(font), trueBG);
						videoCard.flushScreen();
					}
				}
			} else if(zsciiChar == -1)
			{
				terminatingZSCIIChar = -2;
				break;
			} else if(zsciiChar != 0)
			{
				//TODO
				printZSCII(zsciiChar);
				flushBufferAndScreen();

				//Range 'A'-'Z'
				if(zsciiChar > 0x40 && zsciiChar < 0x5B)
					//convert to lower case
					zsciiChar += 0x20;
				if(zsciiChar == 13 || terminatingZSCIIChars.contains(zsciiChar))
				{
					terminatingZSCIIChar = zsciiChar;
					break;
				}
				targetTextBuffer.writeNextEntryByte(0, zsciiChar);
				targetTextBuffer.finishEntry();
			}
		}
		videoCard.hintInputCharUsage(InputUsageHint.COMMAND_END);
		return terminatingZSCIIChar;
	}
	public int inputSingleChar()
	{
		flushBufferAndScreen();
		videoCard.hintInputCharUsage(InputUsageHint.SINGLE_CHAR);
		return videoCard.nextInputChar();
	}
	private void appendToBuffer(int zsciiChar, boolean isSpace)
	{
		int bufferI = outputBufferLength ++;
		if(bufferI == outputBufferChars.length)
		{
			outputBufferChars = Arrays.copyOf(outputBufferChars, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferFonts = Arrays.copyOf(outputBufferFonts, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferStyles = Arrays.copyOf(outputBufferStyles, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferFGs = Arrays.copyOf(outputBufferFGs, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferBGs = Arrays.copyOf(outputBufferBGs, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferWidths = Arrays.copyOf(outputBufferWidths, bufferI + OUTPUT_BUFFER_OVERHEAD);
		}
		if(!isSpace && firstNonSpaceIndex < 0)
			firstNonSpaceIndex = bufferI;

		int font = currentWindowProperties.getProperty(FontNumProp);
		int style = currentWindowProperties.getProperty(TextStyleProp);
		int col = currentWindowProperties.getProperty(ColorDataProp);
		int trueFG = videoCard.getTrueColor(col & 0xFF);
		int trueBG = videoCard.getTrueColor(col >>> 8);

		int width = videoCard.getCharWidth(zsciiChar, font, style);
		outputBufferWidth += width;

		outputBufferChars[bufferI] = zsciiChar;
		outputBufferFonts[bufferI] = font;
		outputBufferStyles[bufferI] = style;
		outputBufferFGs[bufferI] = trueFG;
		outputBufferBGs[bufferI] = trueBG;
		outputBufferWidths[bufferI] = width;
	}
	private void flushBufferAndScreen()
	{
		flushBuffer();
		videoCard.flushScreen();
	}
	public void flushBuffer()
	{
		WindowPropsAttrs properties = currentWindowProperties;
		int bufferPrintIndex;
		if(!extraNL && outputBufferWidth > properties.getProperty(SizeXProp) - properties.getProperty(MarginRProp) - properties.getProperty(CursorXProp))
		{
			if(firstNonSpaceIndex < 0)
				firstNonSpaceIndex = outputBufferLength;
			if(!replaceOnlyFirstSpaceWithExtraNL)
				bufferPrintIndex = firstNonSpaceIndex;
			else if(firstNonSpaceIndex > 0)
				bufferPrintIndex = 1;
			else
				bufferPrintIndex = 0;
			newlineMoveCursor();
			extraNL = true;
		} else
			bufferPrintIndex = 0;

		if(bufferPrintIndex < outputBufferLength)
		{
			extraNL = false;
			do
			{
				int width = outputBufferWidths[bufferPrintIndex];
				//zmach06e.pdf says to use a loop here. This seems wrong.
				if(width > properties.getProperty(SizeXProp) - properties.getProperty(MarginRProp) - properties.getProperty(CursorXProp))
					//We don't need to set extraNL / we shouldn't, because immediately after this NL a character is showed.
					newlineMoveCursor();

				int zsciiChar = outputBufferChars[bufferPrintIndex];
				int font = outputBufferFonts[bufferPrintIndex];
				int style = outputBufferStyles[bufferPrintIndex];
				int trueFG = outputBufferFGs[bufferPrintIndex];
				int trueBG = outputBufferBGs[bufferPrintIndex];
				showCharMoveCursor(zsciiChar, font, style, trueFG, trueBG, width);
				bufferPrintIndex ++;
			} while(bufferPrintIndex < outputBufferLength);
		}

		outputBufferLength = 0;
		outputBufferWidth = 0;
		firstNonSpaceIndex = -1;
	}
	private void showCharMoveCursor(int zsciiChar, int font, int style, int trueFG, int trueBG, int width)
	{
		int oldCursorX = currentWindowProperties.getProperty(CursorXProp);
		int cursorY = currentWindowProperties.getProperty(CursorYProp);
		videoCard.showChar(zsciiChar, font, style, trueFG, trueBG, oldCursorX - 1, cursorY - 1);
		setPropertyCurrentWindow(CursorXProp, oldCursorX + width);
	}
	private void newlineMoveCursor()
	{
		int oldCursorY = currentWindowProperties.getProperty(CursorYProp);
		int fontHeight = currentWindowProperties.getProperty(FontSizeProp) >>> 8;
		int maxCursorY = currentWindowProperties.getProperty(SizeYProp) - fontHeight;
		int newCursorY = oldCursorY + fontHeight;
		if(newCursorY > maxCursorY)
		{
			videoCard.scroll(newCursorY - maxCursorY);
			setPropertyCurrentWindow(CursorYProp, maxCursorY);
		} else
			setPropertyCurrentWindow(CursorYProp, newCursorY);
		setPropertyCurrentWindow(CursorXProp, 1);
		unitsYRemainingToNextMORE --;
		if(unitsYRemainingToNextMORE < 1)
		{
			//TODO print "[MORE]", wait for any key press, delete "[MORE]", reset linesRemainingToNextMORE
		}
	}
}