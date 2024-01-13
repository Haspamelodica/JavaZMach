package net.haspamelodica.javazmach.assembler.model;

import java.util.Optional;

public record SectionDeclaration(ExplicitSection section, Optional<IntegralValue> value) implements ZAssemblerFileEntry
{}
