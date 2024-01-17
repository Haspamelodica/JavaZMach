package net.haspamelodica.javazmach.assembler.model;

import java.util.Optional;

public record Buffer(String name, IntegralValue byteLength, Optional<ByteSequence> optSeq) implements ZAssemblerFileEntry
{}
