package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.values.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public record Buffer(IdentifierDeclaration ident, Optional<IntegralValue> byteLength, Optional<ByteSequence> optSeq) implements MacroOrFileEntry
{}
