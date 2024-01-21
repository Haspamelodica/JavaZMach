package net.haspamelodica.javazmach.assembler.model.entries.globaltable;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public record Global(String name, Optional<IntegralValue> initialValue)
{}
