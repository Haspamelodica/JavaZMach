# Assembler Syntax
The grammar is described in `src/net/haspamelodica/javazmach/assembler/parser/grammar.txt`.
The important basics should be covered here.

## Labels and Names
Labels and variable names must be unique. In particular, routine locals do not shadow globals, their names must differ.
Even worese, locals in different routines may not have the same name. This is due to routines having no defined end point,
hence no clear scoping of variables is possible. An exception is made for names declared in macros.

## Instruction Syntax
```
<opcode mnemonic> [<argument1>[, <argument2>[, ...]]] [? [~] <branch target>] [-> <store target>]
```

Note that the condition of a conditional jump can be negated by prepending a tilde to the branch target,
i.e., the instruction `je var ?~ label` jumps to `label` if `var` is **not** zero.

## Macros
Macros can be called similarly to routines, but instead of performing a routine call, they simply insert their
content wherever they were invoked (as you would expect from a macro).
Named labels/locations/variables declared inside a macro are limited to that macros scope. Hence, when declaring
a routine in a macro, the locals do not clash with locals declared elsewhere. If a macro A invokes another macro B,
then labels/names declared in A can also be used in B.

### Macro Declaration Syntax
```
.MACRO
.MACROPARAM <param1 name>
.MACROPARAM <param2 name>
.MACROPARAM <param3 name>
...

// content

.ENDMACRO
```

#### Macro Parameter Expansion
Parameters that are part of instructions are expanded with `..<param name>`.
Parameters that define a new name (e.g., label declarations) are expanded with `.:<param name>`

### Macro Invocation
Macros are invoked with `.<macro name> [<param1 name>[, <param2 name>[, ...]]]`

## Dictionary
```
.DICTIONARY [<separator1> [<separator2> [...]]]
.KEY <key1> [.DATA <size1> BYTE <VALUE1> [.DATA ...]]
.KEY <key2> [.DATA <size2> BYTE <VALUE2> [.DATA ...]]
...
```

Token separator characters are specified as char literals, e.g., `'+'`.
You do not need to specify any, if the default separators are sufficient for you.
Keys can be specified as string literals.

## Object Table
(TODO)

## Globals
```
.GLOBALS
.GLOBAL <global1> [<default1>]
.GLOBAL <global2> [<default2>]
.GLOBAL <global3> [<default3>]
...
```

## Routines
```
.ROUTINE <routine name>
.LOCAL <local1> [<default1>]
.LOCAL <local2> [<default2>]
.LOCAL <local3> [<default3>]
...

// body
```
