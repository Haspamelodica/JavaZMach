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
		System.out.println(tree.getParent(1));
		//TODO print object tree
	}
}