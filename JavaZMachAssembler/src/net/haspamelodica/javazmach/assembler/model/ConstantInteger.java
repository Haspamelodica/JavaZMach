package net.haspamelodica.javazmach.assembler.model;

import java.math.BigInteger;

public record ConstantInteger(BigInteger value) implements Operand, HeaderValue, BranchTarget, ConstantByteSequenceElement
{}
