package net.haspamelodica.javaz.core.io;

import net.haspamelodica.javaz.core.memory.ReadOnlyByteSet;
import net.haspamelodica.javaz.core.memory.SequentialRWMemoryAccess;

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
	 * Input runs until <code>maxZSCIIChars</code> ZSCII chars have been read,
	 * or ZSCII 13 or one of <code>terminatingZSCIIChars</code> has been read.
	 * The terminating ZSCII character (if any) is not stored.
	 * Returns the terminating ZSCII character,
	 * or -1 if <code>maxZSCIIChars</code> have been read,
	 * or -2 if the end of input has been reached,
	 * or -3 if an IO error occurred.
	 */
	public int inputToTextBuffer(int maxZSCIIChars, SequentialRWMemoryAccess targetTextBuffer, ReadOnlyByteSet terminatingZSCIIChars);
	public WindowPropsAttrs getProperties();
}