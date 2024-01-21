package net.haspamelodica.javazmach.assembler.model;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public record RoutineLocal(String name, Optional<IntegralValue> initialValue)
{}
