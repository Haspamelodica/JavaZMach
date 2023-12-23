package net.haspamelodica.javazmach;

import static net.haspamelodica.javazmach.core.header.HeaderField.DictionaryLoc;
import static net.haspamelodica.javazmach.core.header.HeaderField.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.CopyOnWriteMemory;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javazmach.core.memory.StaticArrayBackedMemory;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverter;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverterNoSpecialChars;
import net.haspamelodica.javazmach.core.text.ZCharStream;
import net.haspamelodica.javazmach.core.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javazmach.core.text.ZCharsToZSCIIConverterStream;
import net.haspamelodica.javazmach.core.text.ZSCIICharZCharConverter;

public class DumpDictionary
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = new GlobalConfig();
		CopyOnWriteMemory mem = new CopyOnWriteMemory(new StaticArrayBackedMemory(Files.readAllBytes(Paths.get("../storyfiles/zork1.z3"))));
		int version = HeaderParser.getFieldUnchecked(mem, Version);
		HeaderParser header = new HeaderParser(config, version, mem);
		SequentialMemoryAccess seqMem = new SequentialMemoryAccess(mem);
		ZSCIICharZCharConverter zsciiZcharConverter = new ZSCIICharZCharConverter(config, version, header, mem);
		ZCharStream zCharsUnpacker = new ZCharsSeqMemUnpacker(seqMem);
		ZCharsToZSCIIConverterStream textConverter = new ZCharsToZSCIIConverterStream(config, version, header, mem, zsciiZcharConverter);
		UnicodeZSCIIConverter unicodeConv = new UnicodeZSCIIConverterNoSpecialChars(config);
		mem.reset();
		zsciiZcharConverter.reset();
		textConverter.reset(zCharsUnpacker);
		seqMem.setAddress(header.getField(DictionaryLoc));
		System.out.print("Word separators: ");
		for(int i = seqMem.readNextByte(); i > 0; i --)
			unicodeConv.zsciiToUnicode(seqMem.readNextByte(), System.out::print);
		System.out.println();
		System.out.println(version > 3 ? "Z-Chars                    ZSCII       Data" : "Z-Chars           ZSCII    Data");
		int dataLength = seqMem.readNextByte() - (version > 3 ? 6 : 4);
		for(int i = seqMem.readNextWord(); i > 0; i --)
		{
			int textualWordStart = seqMem.getAddress();
			zCharsUnpacker.decode(z -> System.out.printf("%2d ", z));
			seqMem.setAddress(textualWordStart);
			System.out.print('"');
			int[] wordLenUnicode = new int[1];
			textConverter.decode(z -> unicodeConv.zsciiToUnicode(z, u ->
			{
				wordLenUnicode[0] ++;
				System.out.print(u);
			}));
			System.out.print('"');
			for(int j = (version > 3 ? 9 : 6) - wordLenUnicode[0]; j > 0; j --)
				System.out.print(' ');
			for(int j = 0; j < dataLength; j ++)
				System.out.printf(" 0x%02x", seqMem.readNextByte());
			System.out.println();
		}
	}
}