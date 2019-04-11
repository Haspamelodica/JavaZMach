package net.haspamelodica.javaz.core.io;

import net.haspamelodica.javaz.core.memory.ReadOnlyByteSet;
import net.haspamelodica.javaz.core.memory.WritableBuffer;

public interface Window
{
	public void scroll(int y);
	public void showChar(CharacterDescription charDescr);
	public void newline();
	public void showPicture(int picture, int x, int y);
	public void erasePicture(int picture, int x, int y);
	public void eraseWindow();
	/**
	 * Stores ZSCII chars as bytes in <code>targetTextBuffer</code> from the current input stream.
	 * Input runs until <code>targetTextBuffer</code> is full,
	 * or ZSCII 13 or one of <code>terminatingZSCIIChars</code> has been read.
	 * The terminating ZSCII character (if any) is not stored.
	 * Returns the terminating ZSCII character,
	 * or -1 if the end of <code>targetTextBuffer</code> has been read.
	 */
	public int inputToTextBuffer(WritableBuffer targetTextBuffer, ReadOnlyByteSet terminatingZSCIIChars);
	public int inputSingleChar();
	public WindowPropsAttrs getProperties();
}