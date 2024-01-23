package net.haspamelodica.javazmach.assembler.model.entries.objecttable;

import net.haspamelodica.javazmach.assembler.model.values.ByteSequence;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public record Property(IntegralValue index, ByteSequence bytes) implements ZObjectEntry
{}
