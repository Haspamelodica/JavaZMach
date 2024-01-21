package net.haspamelodica.javazmach.assembler.model.entries.objecttable;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.values.ByteSequence;

public record Property(BigInteger index, ByteSequence bytes) implements ZObjectEntry
{}
