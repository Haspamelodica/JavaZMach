package net.haspamelodica.javaz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.model.HeaderParser;
import net.haspamelodica.javaz.model.memory.WritableFixedSizeMemory;
import net.haspamelodica.javaz.model.objects.ObjectTree;

public class DumpObjectTree
{
	public static void main(String[] args) throws IOException
	{
		WritableFixedSizeMemory mem = new WritableFixedSizeMemory(Files.readAllBytes(Paths.get("ZORK1.z3")));
		ObjectTree tree = new ObjectTree(new GlobalConfig(), 3, new HeaderParser(mem), mem);
		tree.reset();
		for(int objNumber = 1; objNumber < 255; objNumber ++)
			if(tree.getParent(objNumber) == 0)
			{
				printObjTree(tree, objNumber);
			}
	}
	private static void printObjTree(ObjectTree tree, int topLevelObjNumber)
	{
		boolean[] hasSiblingsStack = new boolean[255];
		for(int objNumber = topLevelObjNumber, depth = 0; objNumber != 0;)
		{
			for(int i = 0; i < depth - 1; i ++)
				System.out.print(hasSiblingsStack[i] ? " |" : "  ");
			if(depth > 0)
				System.out.print(" +");
			System.out.println(objNumber);
			int child = tree.getChild(objNumber);
			int sibling = tree.getSibling(objNumber);
			if(depth > 0)
				hasSiblingsStack[depth - 1] = sibling != 0;
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
				} while(tree.getSibling(objNumber) == 0 && objNumber != 0);
				if(objNumber != 0)
					objNumber = tree.getSibling(objNumber);
			}
		}
	}
}