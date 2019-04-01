package net.haspamelodica.javaz.core.text;

public interface ZCharStreamSource
{
	public void reset();
	public boolean hasNext();
	public byte nextZChar();
}