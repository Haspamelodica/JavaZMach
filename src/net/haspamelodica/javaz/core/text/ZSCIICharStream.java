package net.haspamelodica.javaz.core.text;

public interface ZSCIICharStream
{
	/**
	 * Returns the number of ZSCII chars outputted.
	 */
	public int decode(ZSCIICharStreamReceiver target);
}