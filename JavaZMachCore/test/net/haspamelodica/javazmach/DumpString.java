package net.haspamelodica.javazmach;

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

public class DumpString
{

	public static void main(String[] args) throws IOException
	{
		String path = "../storyfiles/zork1.z3";
		int address = 0x1140c; //address of multiline string
		GlobalConfig config = new GlobalConfig();

		CopyOnWriteMemory mem = new CopyOnWriteMemory(new StaticArrayBackedMemory(Files.readAllBytes(Paths.get(path))));
		HeaderParser header = new HeaderParser(config, HeaderParser.getFieldUnchecked(mem, Version), mem);
		SequentialMemoryAccess memSeq = new SequentialMemoryAccess(mem);
		int version = header.getField(Version);
		ZSCIICharZCharConverter zsciiZcharConverter = new ZSCIICharZCharConverter(config, version, header, mem);
		ZCharStream zCharStream = new ZCharsSeqMemUnpacker(memSeq);
		ZCharsToZSCIIConverterStream textConverter = new ZCharsToZSCIIConverterStream(config, version, header, mem, zsciiZcharConverter);
		zsciiZcharConverter.reset();
		textConverter.reset(target -> zCharStream.decode(zchar ->
		{
			System.out.println("[ZCHAR " + zchar + "]");
			target.accept(zchar);
		}));
		UnicodeZSCIIConverter unicodeConverter = new UnicodeZSCIIConverterNoSpecialChars(config);
		unicodeConverter.resetUnicodeToZSCII();
		memSeq.setAddress(address);
		textConverter.decode(zsciiChar ->
		{
			System.out.printf("%02x ", zsciiChar);
			unicodeConverter.zsciiToUnicode(zsciiChar, System.out::write);
			System.out.println();
		});
	}
}
