package net.haspamelodica.javazmach.assembler.model;

import java.math.BigInteger;
import java.util.Optional;

public record Buffer(String name, BigInteger byteLength, Optional<ByteSequence> optSeq) implements ZAssemblerFileEntry
{}
