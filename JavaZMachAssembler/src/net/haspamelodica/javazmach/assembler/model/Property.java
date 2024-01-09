package net.haspamelodica.javazmach.assembler.model;

import java.math.BigInteger;

public record Property(BigInteger index, ByteSequence bytes) implements ZObjectEntry
{}
