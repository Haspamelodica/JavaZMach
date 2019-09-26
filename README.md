# JavaZMach
A Z-Machine interpreter in Java

Work in progress. Currently this interpreter is able to run ZORK1 (version Z3).<br>
When importing this project in Eclipse, tick "Check for nested projects" in the import dialog.

There are currently two UI backends; located in JavaZMachConsoleUI and JavaZMachSWTUI.<br>
JavaZMachConsoleUI is not perfect as it has to emulate some Z-Machine screen functions.

Sources for contents of storyfiles/ and doc/:
 * [The Z-Machine Standards Document (version 1.1)](http://inform-fiction.org/zmachine/standards/z1point1)
 (Great Z-Machine documentation written by Graham Nelson)
 * [The Quetzal Z-Machine Saved Game Standard](http://inform-fiction.org/zmachine/standards/quetzal)
 ("Quetzal: a common format for saved-game files")
 * [The Z-Machine, and How to Emulate It (version 0.6e)](https://www.ifarchive.org/if-archive/infocom/interpreters/specification/zspec02/zmach06e.pdf)
 (Another Z-Machine documentation. Covers more implementation details)
 * [IFDB - Zork I](https://ifdb.tads.org/viewgame?id=0dbnusxunq7fw5ro)
 * [The Interactive Fiction Archive - Infocom - Interpreters - Tools](http://ifarchive.org/indexes/if-archive/infocom/interpreters/tools/)
 (Most test story files come from this site: CZECH, TerpEtude, strictz)
 * [praxix.z5](http://eblong.com/zarf/ftp/praxix.z5) at [EBLONG - Zarfhome](http://eblong.com/zarf/ftp/). See also http://eblong.com/zarf/ftp/.
 * [trinity.DAT](https://if.illuminion.de/infocom/trinity.DAT) (named trinity.z4 in this repo) at [Illuminion - Infocom](https://if.illuminion.de/infocom.html)
 *  Unfortunately, I can't find the source for sanddancer.z8 anymore. The ["official" website of Sanddancer](http://sand-dancer.textories.com/) doesn't seem to have a .z8 version of this game.

You can find many other games at [Historicalsource](https://github.com/historicalsource/).
