package net.haspamelodica.javaz.core.io;

import static net.haspamelodica.javaz.core.header.HeaderField.DefaultBGCol;
import static net.haspamelodica.javaz.core.header.HeaderField.DefaultFGCol;
import static net.haspamelodica.javaz.core.header.HeaderField.FontHeightV5;
import static net.haspamelodica.javaz.core.header.HeaderField.FontHeightV6;
import static net.haspamelodica.javaz.core.header.HeaderField.FontWidthV5;
import static net.haspamelodica.javaz.core.header.HeaderField.FontWidthV6;
import static net.haspamelodica.javaz.core.header.HeaderField.ScrHeightUnits;
import static net.haspamelodica.javaz.core.header.HeaderField.ScrWidthUnits;
import static net.haspamelodica.javaz.core.header.HeaderField.StatLineType;
import static net.haspamelodica.javaz.core.header.HeaderField.TermCharsTableLoc;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.AttrsProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.BufferedAttr;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.ColorDataProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.CursorXProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.CursorYProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.FontNumProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.FontSizeProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.LineCountProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.LocXProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.LocYProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.MarginLProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.MarginRProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.NLICountProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.NLIRoutineProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.ScrollingAttr;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.SizeXProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.SizeYProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.TextStyleProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.TranscriptAttr;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.TrueBGProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.TrueFGProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.WrappingAttr;

import java.util.Arrays;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.header.HeaderParser;
import net.haspamelodica.javaz.core.memory.ReadOnlyMemory;
import net.haspamelodica.javaz.core.memory.WritableBuffer;
import net.haspamelodica.javaz.core.memory.ZeroTerminatedReadOnlyByteSet;
import net.haspamelodica.javaz.core.text.UnicodeZSCIIConverter;
import net.haspamelodica.javaz.core.text.ZSCIICharStream;

public class IOCard
{
	private static final int OUTPUT_BUFFER_OVERHEAD = 20;

	private final int version;

	private final boolean replaceAllSpacesWithExtraNL;

	private final HeaderParser			headerParser;
	private final UnicodeZSCIIConverter	unicodeConv;
	private final VideoCard				videoCard;
	private final WindowPropsAttrs[]	windowProperties;

	private boolean isTimeGame;

	private boolean	extraNL;
	private int		firstNonSpaceIndex;

	private char[]	outputBufferChars;
	private int[]	outputBufferFonts;
	private int[]	outputBufferStyles;
	private int[]	outputBufferFGs;
	private int[]	outputBufferBGs;
	private int[]	outputBufferWidths;
	private int		outputBufferLength;
	private int		outputBufferWidth;

	private WindowPropsAttrs currentWindowProperties;

	private final ZeroTerminatedReadOnlyByteSet terminatingZSCIIChars;

	public IOCard(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, VideoCardDefinition vCardDef)
	{
		this.version = version;

		this.replaceAllSpacesWithExtraNL = config.getBool("io.wrapping.replace_all_spaces");

		this.headerParser = headerParser;
		this.unicodeConv = new UnicodeZSCIIConverter(config);
		this.videoCard = vCardDef.create(config, version, headerParser, unicodeConv);
		int windowCount = version < 3 ? 1 : version == 6 ? 8 : 2;
		this.windowProperties = new WindowPropsAttrs[windowCount];
		for(int w = 0; w < windowCount; w ++)
			windowProperties[w] = new WindowPropsAttrs();

		this.outputBufferChars = new char[OUTPUT_BUFFER_OVERHEAD];
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
		int defaultTrueFG = videoCard.getDefaultTrueFG();
		int defaultTrueBG = videoCard.getDefaultTrueFG();
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
			p.setProperty(CursorYProp, 1);
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
				appendToBuffer(unicodeConv.zsciiToUnicodeNoNL(zsciiChar), isSpace);
			if(properties.getAttribute(BufferedAttr) == 0)
				flushBuffer();
		}
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
	public int inputToTextBuffer(WritableBuffer targetTextBuffer)
	{
		flushBuffer();
		int maxZSCIIChars = targetTextBuffer.getCapacity();
		for(int read = 0; read < maxZSCIIChars; read ++)
		{
			int nextChar = videoCard.inputSingleChar();
			if(nextChar == -1)
				return -2;
			int zsciiChar = unicodeConv.unicodeToZSCII((char) nextChar);
			//Range 'A'-'Z'
			if(zsciiChar > 0x40 && zsciiChar < 0x5B)
				//convert to lower case
				zsciiChar += 0x20;
			if(zsciiChar != -1)
			{
				if(zsciiChar == 13 || terminatingZSCIIChars.contains(zsciiChar))
					return zsciiChar;
				targetTextBuffer.writeNextEntryByte(0, zsciiChar);
				targetTextBuffer.finishEntry();
			}
		}
		return -1;
	}
	public int inputSingleChar()
	{
		flushBuffer();
		return videoCard.inputSingleChar();
	}

	private void appendToBuffer(char unicodeChar, boolean isSpace)
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

		int width = videoCard.getCharWidth(unicodeChar, font, style);
		outputBufferWidth += width;

		outputBufferChars[bufferI] = unicodeChar;
		outputBufferFonts[bufferI] = font;
		outputBufferStyles[bufferI] = style;
		outputBufferFGs[bufferI] = trueFG;
		outputBufferBGs[bufferI] = trueBG;
		outputBufferWidths[bufferI] = width;
	}
	private void flushBuffer()
	{
		WindowPropsAttrs properties = currentWindowProperties;
		int bufferPrintIndex;
		if(!extraNL && outputBufferWidth > properties.getProperty(SizeXProp) - properties.getProperty(MarginRProp) - properties.getProperty(CursorXProp))
		{
			if(firstNonSpaceIndex < 0)
				firstNonSpaceIndex = outputBufferLength;
			if(replaceAllSpacesWithExtraNL)
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

				char unicodeChar = outputBufferChars[bufferPrintIndex];
				int font = outputBufferFonts[bufferPrintIndex];
				int style = outputBufferStyles[bufferPrintIndex];
				int trueFG = outputBufferFGs[bufferPrintIndex];
				int trueBG = outputBufferBGs[bufferPrintIndex];
				showCharMoveCursor(unicodeChar, font, style, trueFG, trueBG, width);
				bufferPrintIndex ++;
			} while(bufferPrintIndex < outputBufferLength);
		}

		outputBufferLength = 0;
		outputBufferWidth = 0;
		firstNonSpaceIndex = -1;
	}
	private void showCharMoveCursor(char unicodeChar, int font, int style, int trueFG, int trueBG, int width)
	{
		int oldCursorX = currentWindowProperties.getProperty(CursorXProp);
		int oldCursorY = currentWindowProperties.getProperty(CursorYProp);
		videoCard.showChar(unicodeChar, font, style, trueFG, trueBG, oldCursorX, oldCursorY);
		currentWindowProperties.setProperty(CursorXProp, oldCursorX + width);
	}
	private void newlineMoveCursor()
	{
		int oldCursorY = currentWindowProperties.getProperty(CursorYProp);
		int maxCursorY = currentWindowProperties.getProperty(SizeYProp);
		int fontHeight = currentWindowProperties.getProperty(FontSizeProp) >>> 8;
		int newCursorY = oldCursorY + fontHeight;
		if(newCursorY > maxCursorY)
			videoCard.scroll(newCursorY - maxCursorY);
		else
			currentWindowProperties.setProperty(CursorYProp, newCursorY);
		currentWindowProperties.setProperty(CursorXProp, 1);
	}
}