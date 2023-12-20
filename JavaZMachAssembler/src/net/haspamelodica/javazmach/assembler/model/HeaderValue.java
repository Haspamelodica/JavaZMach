package net.haspamelodica.javazmach.assembler.model;

public sealed interface HeaderValue permits ConstantByteSequence, ConstantInteger, Label
{}
