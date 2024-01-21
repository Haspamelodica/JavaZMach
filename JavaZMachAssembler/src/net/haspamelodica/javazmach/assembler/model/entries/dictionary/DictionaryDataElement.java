package net.haspamelodica.javazmach.assembler.model.entries.dictionary;

import java.math.BigInteger;

import net.haspamelodica.javazmach.assembler.model.values.HeaderValue;

public record DictionaryDataElement(BigInteger size, HeaderValue value)
{}
