package net.haspamelodica.javaz;

import static net.haspamelodica.javaz.core.header.HeaderField.AbbrevTableLoc;
import static net.haspamelodica.javaz.core.header.HeaderField.AlphabetTableLoc;
import static net.haspamelodica.javaz.core.header.HeaderField.BoldfaceAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.ColorsAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.ColorsReq;
import static net.haspamelodica.javaz.core.header.HeaderField.DefaultBGCol;
import static net.haspamelodica.javaz.core.header.HeaderField.DefaultFGCol;
import static net.haspamelodica.javaz.core.header.HeaderField.DictionaryLoc;
import static net.haspamelodica.javaz.core.header.HeaderField.FileChecksum;
import static net.haspamelodica.javaz.core.header.HeaderField.FileLength;
import static net.haspamelodica.javaz.core.header.HeaderField.FixedSpaceAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.Flags1;
import static net.haspamelodica.javaz.core.header.HeaderField.Flags2;
import static net.haspamelodica.javaz.core.header.HeaderField.FontHeightV5;
import static net.haspamelodica.javaz.core.header.HeaderField.FontHeightV6;
import static net.haspamelodica.javaz.core.header.HeaderField.FontWidthV5;
import static net.haspamelodica.javaz.core.header.HeaderField.FontWidthV6;
import static net.haspamelodica.javaz.core.header.HeaderField.ForceFixedPitchPrint;
import static net.haspamelodica.javaz.core.header.HeaderField.GlobalVarTableLoc;
import static net.haspamelodica.javaz.core.header.HeaderField.HeaderExtTableLoc;
import static net.haspamelodica.javaz.core.header.HeaderField.HighMemoryBase;
import static net.haspamelodica.javaz.core.header.HeaderField.InitialPC15;
import static net.haspamelodica.javaz.core.header.HeaderField.InitialPC78;
import static net.haspamelodica.javaz.core.header.HeaderField.InterpreterNum;
import static net.haspamelodica.javaz.core.header.HeaderField.InterpreterVer;
import static net.haspamelodica.javaz.core.header.HeaderField.ItalicAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.MainLoc;
import static net.haspamelodica.javaz.core.header.HeaderField.MenusReq;
import static net.haspamelodica.javaz.core.header.HeaderField.MouseReq;
import static net.haspamelodica.javaz.core.header.HeaderField.ObjTableLoc;
import static net.haspamelodica.javaz.core.header.HeaderField.OutSt3WidthPixels;
import static net.haspamelodica.javaz.core.header.HeaderField.PicDisplayingAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.PicsReq;
import static net.haspamelodica.javaz.core.header.HeaderField.ReleaseNumber;
import static net.haspamelodica.javaz.core.header.HeaderField.RoutinesOff;
import static net.haspamelodica.javaz.core.header.HeaderField.ScrHeightLines;
import static net.haspamelodica.javaz.core.header.HeaderField.ScrHeightUnits;
import static net.haspamelodica.javaz.core.header.HeaderField.ScrRedrawControl;
import static net.haspamelodica.javaz.core.header.HeaderField.ScrSplitAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.ScrWidthChars;
import static net.haspamelodica.javaz.core.header.HeaderField.ScrWidthUnits;
import static net.haspamelodica.javaz.core.header.HeaderField.SoundFXAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.SoundFXReq;
import static net.haspamelodica.javaz.core.header.HeaderField.StandardRev;
import static net.haspamelodica.javaz.core.header.HeaderField.StatLineNotAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.StatLineType;
import static net.haspamelodica.javaz.core.header.HeaderField.StaticMemBase;
import static net.haspamelodica.javaz.core.header.HeaderField.StoryfileSplit;
import static net.haspamelodica.javaz.core.header.HeaderField.StringsOff;
import static net.haspamelodica.javaz.core.header.HeaderField.Tandy;
import static net.haspamelodica.javaz.core.header.HeaderField.TermCharsTableLoc;
import static net.haspamelodica.javaz.core.header.HeaderField.TimedKeyInputAvail;
import static net.haspamelodica.javaz.core.header.HeaderField.Transcipting;
import static net.haspamelodica.javaz.core.header.HeaderField.UndoReq;
import static net.haspamelodica.javaz.core.header.HeaderField.VarPitchFontDefault;
import static net.haspamelodica.javaz.core.header.HeaderField.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.core.header.HeaderField;
import net.haspamelodica.javaz.core.header.HeaderParser;
import net.haspamelodica.javaz.core.memory.CopyOnWriteMemory;
import net.haspamelodica.javaz.core.memory.StaticArrayBackedMemory;
import net.haspamelodica.javaz.core.memory.WritableMemory;

public class DumpHeader
{
	private static WritableMemory mem;
	public static void main(String[] args) throws IOException
	{
		mem = new CopyOnWriteMemory(new StaticArrayBackedMemory(Files.readAllBytes(Paths.get("../storyfiles/zork1.z3"))));
		print("Version", Version);
		print("Flags1", Flags1);
		print("ColorsAvail", ColorsAvail);
		print("StatLineType", StatLineType);
		print("PicDisplayingAvail", PicDisplayingAvail);
		print("StoryfileSplit", StoryfileSplit);
		print("BoldfaceAvail", BoldfaceAvail);
		print("Tandy", Tandy);
		print("ItalicAvail", ItalicAvail);
		print("StatLineNotAvail", StatLineNotAvail);
		print("FixedSpaceAvail", FixedSpaceAvail);
		print("ScrSplitAvail", ScrSplitAvail);
		print("SoundFXAvail", SoundFXAvail);
		print("VarPitchFontDefault", VarPitchFontDefault);
		print("TimedKeyInputAvail", TimedKeyInputAvail);
		print("ReleaseNumber", ReleaseNumber);
		print("HighMemoryBase", HighMemoryBase);
		print("InitialPC15", InitialPC15);
		print("MainLoc", MainLoc);
		print("InitialPC78", InitialPC78);
		print("DictionaryLoc", DictionaryLoc);
		print("ObjTableLoc", ObjTableLoc);
		print("GlobalVarTableLoc", GlobalVarTableLoc);
		print("StaticMemBase", StaticMemBase);
		print("Flags2", Flags2);
		print("Transcipting", Transcipting);
		print("ForceFixedPitchPrint", ForceFixedPitchPrint);
		print("ScrRedrawControl", ScrRedrawControl);
		print("PicsReq", PicsReq);
		print("UndoReq", UndoReq);
		print("MouseReq", MouseReq);
		print("ColorsReq", ColorsReq);
		print("SoundFXReq", SoundFXReq);
		print("MenusReq", MenusReq);
		print("AbbrevTableLoc", AbbrevTableLoc);
		print("FileLength", FileLength);
		print("FileChecksum", FileChecksum);
		print("InterpreterNum", InterpreterNum);
		print("InterpreterVer", InterpreterVer);
		print("ScrHeightLines", ScrHeightLines);
		print("ScrWidthChars", ScrWidthChars);
		print("ScrWidthUnits", ScrWidthUnits);
		print("ScrHeightUnits", ScrHeightUnits);
		print("FontWidthV5", FontWidthV5);
		print("FontHeightV6", FontHeightV6);
		print("FontHeightV5", FontHeightV5);
		print("FontWidthV6", FontWidthV6);
		print("RoutinesOff", RoutinesOff);
		print("StringsOff", StringsOff);
		print("DefaultBGCol", DefaultBGCol);
		print("DefaultFGCol", DefaultFGCol);
		print("TermCharsTableLoc", TermCharsTableLoc);
		print("OutSt3WidthPixels", OutSt3WidthPixels);
		print("StandardRev", StandardRev);
		print("AlphabetTableLoc", AlphabetTableLoc);
		print("HeaderExtTableLoc", HeaderExtTableLoc);

		//TODO set field
	}
	private static void print(String descr, HeaderField field)
	{
		System.out.printf("%4x", HeaderParser.getFieldUnchecked(mem, field));
		System.out.print(": ");
		System.out.println(descr);
	}
}