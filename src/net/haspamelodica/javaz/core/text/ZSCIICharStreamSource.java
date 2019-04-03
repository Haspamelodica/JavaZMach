package net.haspamelodica.javaz.core.text;

public interface ZSCIICharStreamSource
{
	public void reset();
	public boolean hasNext();
	public byte nextZSCIIChar();
}