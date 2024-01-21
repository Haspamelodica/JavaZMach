package net.haspamelodica.javazmach.assembler.model.values;

import java.math.BigInteger;

public record NumberLiteral(BigInteger value) implements IntegralLiteral
{}
