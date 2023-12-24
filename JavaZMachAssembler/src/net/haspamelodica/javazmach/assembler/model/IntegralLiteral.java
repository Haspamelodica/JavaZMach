package net.haspamelodica.javazmach.assembler.model;

public sealed interface IntegralLiteral extends IntegralValue, ByteSequenceElement permits NumberLiteral, CharLiteral
{}
