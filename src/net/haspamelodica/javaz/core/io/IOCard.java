package net.haspamelodica.javaz.core.io;

import static net.haspamelodica.javaz.core.HeaderParser.StatLineTypeLoc;
import static net.haspamelodica.javaz.core.HeaderParser.TermCharsTableLocLoc;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.BufferedAttr;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.ColorDataProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.CursorXProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.FontNumProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.MarginLProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.MarginRProp;
import static net.haspamelodica.javaz.core.io.WindowPropsAttrs.SizeYProp;

import java.util.Arrays;

import net.haspamelodica.javaz.GlobalConfig;
import net.haspamelodica.javaz.core.HeaderParser;
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
	private final Window[]				windows;

	private boolean isTimeGame;

	private boolean	extraNL;
	private int		firstNonSpaceIndex;
	private char[]	outputBufferChars;
	private int[]	outputBufferFonts;
	private int[]	outputBufferFGs;
	private int[]	outputBufferBGs;
	private int[]	outputBufferWidths;
	private int		outputBufferLength;
	private int		outputBufferWidth;
	private Window	currentWindow;

	private final CharacterDescription			charDescrBuf;
	private final ZeroTerminatedReadOnlyByteSet	terminatingZSCIIChars;

	public IOCard(GlobalConfig config, int version, HeaderParser headerParser, ReadOnlyMemory mem, VideoCardDefinition vCardDef)
	{
		this.version = version;

		this.replaceAllSpacesWithExtraNL = config.getBool("io.wrapping.replace_all_spaces");

		this.headerParser = headerParser;
		this.unicodeConv = new UnicodeZSCIIConverter(config);
		this.videoCard = vCardDef.create(config, version, headerParser, unicodeConv);
		int windowCount = version < 3 ? 1 : version == 6 ? 8 : 2;
		this.windows = new Window[windowCount];
		for(int w = 0; w < windowCount; w ++)
			windows[w] = videoCard.createWindow();

		this.outputBufferChars = new char[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferFonts = new int[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferFGs = new int[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferBGs = new int[OUTPUT_BUFFER_OVERHEAD];
		this.outputBufferWidths = new int[OUTPUT_BUFFER_OVERHEAD];

		this.charDescrBuf = new CharacterDescription();
		this.terminatingZSCIIChars = new ZeroTerminatedReadOnlyByteSet(mem);
	}

	public void reset()
	{
		isTimeGame = version == 3 && headerParser.getField(StatLineTypeLoc) == 1;
		extraNL = false;
		outputBufferLength = 0;
		firstNonSpaceIndex = -1;
		currentWindow = windows[0];//TODO is this correct for all versions?
		int terminatingCharsTableLoc = headerParser.getField(TermCharsTableLocLoc);
		terminatingZSCIIChars.setStartAddr(terminatingCharsTableLoc == 0 ? -1 : terminatingCharsTableLoc);
	}

	public void printZSCII(int zsciiChar)
	{
		boolean isSpace = zsciiChar == 9 || zsciiChar == 11 || zsciiChar == 32;
		if(zsciiChar == 13)
		{
			flushBuffer();
			extraNL = false;
			currentWindow.newline();
		} else
		{
			WindowPropsAttrs properties = currentWindow.getProperties();
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
		return currentWindow.inputToTextBuffer(targetTextBuffer, terminatingZSCIIChars);
	}
	public int inputSingleChar()
	{
		flushBuffer();
		return currentWindow.inputSingleChar();
	}

	private void appendToBuffer(char unicodeChar, boolean isSpace)
	{
		int bufferI = outputBufferLength ++;
		if(bufferI == outputBufferChars.length)
		{
			outputBufferChars = Arrays.copyOf(outputBufferChars, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferFonts = Arrays.copyOf(outputBufferFonts, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferFGs = Arrays.copyOf(outputBufferFGs, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferBGs = Arrays.copyOf(outputBufferBGs, bufferI + OUTPUT_BUFFER_OVERHEAD);
			outputBufferWidths = Arrays.copyOf(outputBufferWidths, bufferI + OUTPUT_BUFFER_OVERHEAD);
		}
		if(!isSpace && firstNonSpaceIndex < 0)
			firstNonSpaceIndex = bufferI;

		int font = currentWindow.getProperties().getProperty(FontNumProp);
		int col = currentWindow.getProperties().getProperty(ColorDataProp);
		int trueFG = videoCard.getTrueColor(col & 0xFF);
		int trueBG = videoCard.getTrueColor(col >>> 8);

		charDescrBuf.unicodeChar = unicodeChar;
		charDescrBuf.fontNumber = font;
		charDescrBuf.trueForegroundColor = trueFG;
		charDescrBuf.trueBackgroundColor = trueBG;
		int width = videoCard.getCharWidth(charDescrBuf);
		outputBufferWidth += width;

		outputBufferChars[bufferI] = unicodeChar;
		outputBufferFonts[bufferI] = font;
		outputBufferFGs[bufferI] = trueFG;
		outputBufferBGs[bufferI] = trueBG;
		outputBufferWidths[bufferI] = width;
	}
	private void flushBuffer()
	{
		WindowPropsAttrs properties = currentWindow.getProperties();
		int bufferPrintIndex;
		if(!extraNL && outputBufferWidth > properties.getProperty(SizeYProp) - properties.getProperty(MarginRProp) - properties.getProperty(CursorXProp))
		{
			if(firstNonSpaceIndex < 0)
				firstNonSpaceIndex = outputBufferLength;
			if(replaceAllSpacesWithExtraNL)
				bufferPrintIndex = firstNonSpaceIndex;
			else if(firstNonSpaceIndex > 0)
				bufferPrintIndex = 1;
			else
				bufferPrintIndex = 0;
			currentWindow.newline();
			extraNL = true;
		} else
			bufferPrintIndex = 0;

		if(bufferPrintIndex < outputBufferLength)
		{
			extraNL = false;
			do
			{
				//zmach06e.pdf says to use a loop here. This seems wrong.
				if(outputBufferWidths[bufferPrintIndex] > properties.getProperty(SizeYProp) - properties.getProperty(MarginRProp) - properties.getProperty(CursorXProp))
					//We don't need to set extraNL / we shouldn't, because immediately after this NL a character is showed.
					currentWindow.newline();

				charDescrBuf.unicodeChar = outputBufferChars[bufferPrintIndex];
				charDescrBuf.fontNumber = outputBufferFonts[bufferPrintIndex];
				charDescrBuf.trueForegroundColor = outputBufferFGs[bufferPrintIndex];
				charDescrBuf.trueBackgroundColor = outputBufferBGs[bufferPrintIndex];
				currentWindow.showChar(charDescrBuf);
				bufferPrintIndex ++;
			} while(bufferPrintIndex < outputBufferLength);
		}

		outputBufferLength = 0;
		outputBufferWidth = 0;
		firstNonSpaceIndex = -1;
	}
}