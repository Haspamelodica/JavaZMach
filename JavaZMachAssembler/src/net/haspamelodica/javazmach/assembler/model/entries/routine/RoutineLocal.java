package net.haspamelodica.javazmach.assembler.model.entries.routine;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.entries.IdentifierDeclaration;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public record RoutineLocal(IdentifierDeclaration ident, Optional<IntegralValue> initialValue)
{}
