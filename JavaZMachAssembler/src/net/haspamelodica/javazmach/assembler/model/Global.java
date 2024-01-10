package net.haspamelodica.javazmach.assembler.model;

import java.util.Optional;

public record Global(String name, Optional<IntegralValue> initialValue)
{}
