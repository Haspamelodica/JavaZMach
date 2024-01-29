# Assembler Syntax
The grammar is described in `src/net/haspamelodica/javazmach/assembler/parser/grammar.txt`.
The important basics should be covered here.

## Instruction Blacklist
The following instructions are weirdly encoded and not currently supported:
```
inc, dec, inc_chk, dec_chk, store, pull, load, jump
```

## Labels and Names
Labels and variable names must be unique. In particular, routine locals do not shadow globals, their names must differ.
Even worse, locals in different routines may not have the same name. This is due to routines having no defined end point,
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
Parameters that reference a value are expanded with `..<param name>`.

Parameters that define a new name (e.g., label declarations) are expanded with `.:<param name>`

### Macro Invocation
Macros are invoked with `.<macro name> [<param1>[, <param2>[, ...]]]`

## Values
```
.VALUE <ident> <expr>
```

Values are named constants. They can be arithmetic expressions of constants and other values.
Values can be used in most places, like instruction operands, buffer size, attribute and property indices.
Note: they cannot currently be used as the value of a property declaration. There is no good
reason for this, we just have not gotten around to it yet.

## Buffers
```
.BUFFER [<N> BYTE] <value> [<value>]
```

A buffer is basically just a named region of memory. The size or value must be specified,
otherwise the size cannot be deduced.

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
```
.OBJECTS
.PROPERTY <index> <byte sequence>
.PROPERTY <index> <byte sequence>
...

.OBJECT <ident> "<obj1_name>" {
    .ATTRIBUTE <index>
    ...

    .PROPERTY <index> <byte sequence>
    ...

    .OBJECT <ident> "<obj1_child1_name>" {
        ...
    }
    ...
}
```

Properties declared outside of objects are default property values.
If a default property is not explicitly specified, the default value is 0.
Objects have an identifier, which can be used to refer to them as operands
to instructions. In addition, the name is how they are emitted by the
`print_obj` istruction. Objects declared in other objects are their children
in the object tree.

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
