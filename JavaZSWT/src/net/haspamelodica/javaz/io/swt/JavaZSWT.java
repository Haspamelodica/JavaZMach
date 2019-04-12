package net.haspamelodica.javaz.io.swt;

import java.io.IOException;

import net.haspamelodica.javaz.JavaZ;

public class JavaZSWT
{
	public static void main(String[] args) throws IOException
	{
		JavaZ.run(args, SWTVideoCard::new);
	}
}