package net.haspamelodica.javazmach.assembler.model;

import java.math.BigInteger;

public record NumberLiteral(BigInteger value) implements IntegralLiteral
{}
