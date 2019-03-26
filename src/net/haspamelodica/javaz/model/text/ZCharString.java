package net.haspamelodica.javaz.model.text;

public class ZCharString
{
	public byte[]	chars;
	public int		length;

	public ZCharString()
	{
		chars = new byte[100];
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		result.append('"');
		if(length > 0)
		{
			result.append(chars[0]);
			for(int i = 1; i < length; i ++)
			{
				result.append(',');
				result.append(chars[i]);
			}
		}
		result.append('"');
		return result.toString();
	}
}