package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface IntegralLiteral extends IntegralValue, ByteSequenceElement permits NumberLiteral, CharLiteral
{}
