package net.haspamelodica.javaz.model.text;

public interface ZCharStreamSource
{
	public void reset();
	public boolean hasNext();
	public byte nextZChar();
}