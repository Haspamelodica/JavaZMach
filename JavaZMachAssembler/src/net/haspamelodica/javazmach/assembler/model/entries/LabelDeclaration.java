package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.values.AlignmentValue;

public record LabelDeclaration(IdentifierDeclaration ident, Optional<AlignmentValue> alignment) implements MacroOrFileEntry
{}
