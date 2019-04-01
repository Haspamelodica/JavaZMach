package net.haspamelodica.javaz;

import static net.haspamelodica.javaz.core.HeaderParser.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.haspamelodica.javaz.core.HeaderParser;
import net.haspamelodica.javaz.core.memory.WritableFixedSizeMemory;

public class TestHeaderParser
{
	private static HeaderParser p;
	public static void main(String[] args) throws IOException
	{
		p = new HeaderParser(new WritableFixedSizeMemory(Files.readAllBytes(Paths.get("ZORK1.z3"))));
		print("Version", VersionLoc);
		print("Flags1", Flags1Loc);
		print("ColorsAvail", ColorsAvailLoc);
		print("StatLineType", StatLineTypeLoc);
		print("PicDisplayingAvail", PicDisplayingAvailLoc);
		print("StoryfileSplit", StoryfileSplitLoc);
		print("BoldfaceAvail", BoldfaceAvailLoc);
		print("Tandy", TandyLoc);
		print("ItalicAvail", ItalicAvailLoc);
		print("StatLineNotAvail", StatLineNotAvailLoc);
		print("FixedSpaceAvail", FixedSpaceAvailLoc);
		print("ScrSplitAvail", ScrSplitAvailLoc);
		print("SoundFXAvail", SoundFXAvailLoc);
		print("VarPitchFontDefault", VarPitchFontDefaultLoc);
		print("TimedKeyInputAvail", TimedKeyInputAvailLoc);
		print("ReleaseNumber", ReleaseNumberLoc);
		print("HighMemoryBase", HighMemoryBaseLoc);
		print("InitialPC", InitialPCLoc);
		print("MainLoc", MainLocLoc);
		print("DictionaryLoc", DictionaryLocLoc);
		print("ObjTableLoc", ObjTableLocLoc);
		print("GlobalVarTableLoc", GlobalVarTableLocLoc);
		print("StaticMemBase", StaticMemBaseLoc);
		print("Flags2", Flags2Loc);
		print("Transcipting", TransciptingLoc);
		print("ForceFixedPitchPrint", ForceFixedPitchPrintLoc);
		print("ScrRedrawControl", ScrRedrawControlLoc);
		print("PicsReq", PicsReqLoc);
		print("UndoReq", UndoReqLoc);
		print("MouseReq", MouseReqLoc);
		print("ColorsReq", ColorsReqLoc);
		print("SoundFXReq", SoundFXReqLoc);
		print("MenusReq", MenusReqLoc);
		print("AbbrevTableLoc", AbbrevTableLocLoc);
		print("FileLength", FileLengthLoc);
		print("FileChecksum", FileChecksumLoc);
		print("InterpreterNum", InterpreterNumLoc);
		print("InterpreterVer", InterpreterVerLoc);
		print("ScrHeightLines", ScrHeightLinesLoc);
		print("ScrWidthChars", ScrWidthCharsLoc);
		print("ScrWidthUnits", ScrWidthUnitsLoc);
		print("ScrHeightUnits", ScrHeightUnitsLoc);
		print("FontWidthV5", FontWidthV5Loc);
		print("FontHeightV6", FontHeightV6Loc);
		print("FontHeightV5", FontHeightV5Loc);
		print("FontWidthV6", FontWidthV6Loc);
		print("RoutinesOff", RoutinesOffLoc);
		print("StringsOff", StringsOffLoc);
		print("DefaultBGCol", DefaultBGColLoc);
		print("DefaultFGCol", DefaultFGColLoc);
		print("TermCharsTableLoc", TermCharsTableLocLoc);
		print("OutSt3WidthPixels", OutSt3WidthPixelsLoc);
		print("StandardRev", StandardRevLoc);
		print("AlphabetTableLoc", AlphabetTableLocLoc);
		print("HeaderExtTableLoc", HeaderExtTableLocLoc);

		//TODO set field
	}
	private static void print(String descr, int loc)
	{
		System.out.printf("%4x", p.getField(loc));
		System.out.print(": ");
		System.out.println(descr);
	}
}