package net.haspamelodica.javazmach.assembler.model;

import java.util.Optional;

public record SectionDeclaration(Section section, Optional<IntegralValue> value) implements ZAssemblerFileEntry
{}
