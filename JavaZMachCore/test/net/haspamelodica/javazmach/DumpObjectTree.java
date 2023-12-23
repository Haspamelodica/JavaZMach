package net.haspamelodica.javazmach;

import static net.haspamelodica.javazmach.core.header.HeaderField.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javazmach.core.header.HeaderParser;
import net.haspamelodica.javazmach.core.memory.CopyOnWriteMemory;
import net.haspamelodica.javazmach.core.memory.SequentialMemoryAccess;
import net.haspamelodica.javazmach.core.memory.StaticArrayBackedMemory;
import net.haspamelodica.javazmach.core.objects.ObjectTree;
import net.haspamelodica.javazmach.core.text.ZCharsSeqMemUnpacker;
import net.haspamelodica.javazmach.core.text.ZCharsToZSCIIConverterStream;
import net.haspamelodica.javazmach.core.text.ZSCIICharZCharConverter;

public class DumpObjectTree
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = new GlobalConfig();
		CopyOnWriteMemory mem = new CopyOnWriteMemory(new StaticArrayBackedMemory(Files.readAllBytes(Paths.get("../storyfiles/zork1.z3"))));
		HeaderParser header = new HeaderParser(config, HeaderParser.getFieldUnchecked(mem, Version), mem);
		SequentialMemoryAccess textConvSeqMem = new SequentialMemoryAccess(mem);
		int version = header.getField(Version);
		ObjectTree tree = new ObjectTree(config, version, header, mem);
		ZSCIICharZCharConverter zsciiZcharConverter = new ZSCIICharZCharConverter(config, version, header, mem);
		ZCharsSeqMemUnpacker zCharStream = new ZCharsSeqMemUnpacker(textConvSeqMem);
		ZCharsToZSCIIConverterStream textConv = new ZCharsToZSCIIConverterStream(config, version, header, mem, zsciiZcharConverter);
		zsciiZcharConverter.reset();
		textConv.reset(zCharStream);
		tree.reset();
		for(int objNumber = 1; objNumber < 255; objNumber ++)
			if(tree.getParent(objNumber) == 0)
				printObjTree(version, tree, objNumber, textConvSeqMem, textConv);
	}
	private static void printObjTree(int version, ObjectTree tree, int topLevelObjNumber, SequentialMemoryAccess seqMem, ZCharsToZSCIIConverterStream textConv)
	{
		boolean[] hasSiblingsStack = new boolean[255];
		for(int objNumber = topLevelObjNumber, depth = 0; objNumber != 0;)
		{
			printLineStart(hasSiblingsStack, depth, true);
			System.out.print(objNumber);
			System.out.print(" \"");
			if(tree.getObjectNameWords(objNumber) != 0)
			{
				seqMem.setAddress(tree.getObjectNameLoc(objNumber));
				textConv.decode(zsciiChar -> System.out.print((char) zsciiChar));
			}
			System.out.print('"');
			System.out.println();

			int child = tree.getChild(objNumber);
			int sibling = tree.getSibling(objNumber);
			if(depth > 0)
				hasSiblingsStack[depth - 1] = sibling != 0;
			hasSiblingsStack[depth] = child != 0;

			depth ++;

			printLineStart(hasSiblingsStack, depth, false);
			System.out.print(" attrs:");
			for(int attrNum = version > 3 ? 47 : 31; attrNum >= 0; attrNum --)
				if(tree.getAttribute(objNumber, attrNum) == 1)
					System.out.printf(" %2d", attrNum);
			System.out.println();


			for(int propAddr = tree.getFirstPropAddr(objNumber); propAddr != -1; propAddr = tree.getNextPropAddr(propAddr))
			{
				int propNumber = tree.getPropNumber(propAddr);
				int propSize = tree.getPropSize(propAddr);
				printLineStart(hasSiblingsStack, depth, false);
				System.out.printf(" prop %2d:", propNumber);
				seqMem.setAddress(propAddr);
				for(int i = 0; i < propSize; i ++)
					System.out.printf(" 0x%02x", seqMem.readNextByte());
				System.out.println();
			}

			depth --;

			if(child != 0)
			{
				objNumber = child;
				depth ++;
			} else if(sibling != 0)
				objNumber = sibling;
			else
			{
				do
				{
					objNumber = tree.getParent(objNumber);
					depth --;
				} while(objNumber != 0 && tree.getSibling(objNumber) == 0);
				if(objNumber != 0)
					objNumber = tree.getSibling(objNumber);
			}
		}
	}
	private static void printLineStart(boolean[] hasSiblingsStack, int depth, boolean isFirstLineForThisObject)
	{
		for(int i = 0; i < depth - 1; i ++)
			System.out.print(hasSiblingsStack[i] ? " |" : "  ");
		if(depth > 0)
			System.out.print(isFirstLineForThisObject ? " +" : hasSiblingsStack[depth - 1] ? " |" : "  ");
	}
}