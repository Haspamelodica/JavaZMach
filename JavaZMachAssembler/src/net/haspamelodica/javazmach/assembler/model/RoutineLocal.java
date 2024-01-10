package net.haspamelodica.javazmach.assembler.model;

import java.util.Optional;

public record RoutineLocal(String name, Optional<IntegralValue> initialValue)
{}
