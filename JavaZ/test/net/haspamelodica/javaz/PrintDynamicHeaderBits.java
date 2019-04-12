package net.haspamelodica.javaz;

import net.haspamelodica.javaz.core.header.HeaderParser;
import net.haspamelodica.javaz.core.memory.CopyOnWriteMemory;
import net.haspamelodica.javaz.core.memory.StaticArrayBackedMemory;

public class PrintDynamicHeaderBits
{
	public static void main(String[] args)
	{
		for(int version = 1; version < 9; version ++)
		{
			System.out.println("---Version " + version + "---");
			GlobalConfig config = new GlobalConfig();
			config.setBool("header.dont_allow_undefined_field_write", false);
			HeaderParser headerParser = new HeaderParser(config, version, new CopyOnWriteMemory(new StaticArrayBackedMemory(64)));
			for(int row = 0; row < 16; row ++)
			{
				for(int col = 0; col < 4; col ++)
				{
					for(int bit = 7; bit >= 0; bit --)
						System.out.print(headerParser.isAllowedAsDynamicWrite(row * 4 + col, 1 << bit) ? 'd' : '_');
					System.out.print(' ');
				}
				System.out.println();
			}
		}
	}
}