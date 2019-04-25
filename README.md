# JavaZMach
A Z-Machine interpreter in Java

Work in progress. Currently this interpreter is able to run ZORK1 (version Z3).<br>
When importing this project in Eclipse, tick "Check for nested projects" in the import dialog.

There are currently two UI backends; located in JavaZMachConsoleUI and JavaZMachSWTUI.<br>
JavaZMachConsoleUI is not perfect as it has to emulate some Z-Machine screen functions.

Sources for contents of storyfiles/ and doc/:
 * [The Z-Machine Standards Document (version 1.1)](http://inform-fiction.org/zmachine/standards/z1point1)
 (Great Z-Machine documentation written by Graham Nelson)
 * [The Z-Machine, and How to Emulate It (version 0.6e)](https://www.ifarchive.org/if-archive/infocom/interpreters/specification/zspec02/zmach06e.pdf)
 (Another Z-Machine documentation. Covers more implementation details)
 * [IFDB - Zork I](https://ifdb.tads.org/viewgame?id=0dbnusxunq7fw5ro)
 * [The Interactive Fiction Archive - Infocom - Interpreters - Tools](http://ifarchive.org/indexes/if-archive/infocom/interpreters/tools/)
 (Most test story files come from this site: CZECH, TerpEtude, strictz)
 * [EBLONG - Zarfhome - praxix.z5](http://eblong.com/zarf/ftp/praxix.z5)
 * Unfortunately, I can't find the sources for trinity.z4 and sanddancer.z8 anymore...
 Although it is not my original source, you can download Trinity (and many other Infocom games) from
 [historicalsource](https://github.com/historicalsource): [tr.z4](https://github.com/historicalsource/trinity/blob/master/COMPILED/tr.z4?raw=true)
