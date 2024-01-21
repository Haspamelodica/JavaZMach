package net.haspamelodica.javazmach.assembler.model.entries;

import java.util.Optional;

import net.haspamelodica.javazmach.assembler.model.ExplicitSection;
import net.haspamelodica.javazmach.assembler.model.values.IntegralValue;

public record SectionDeclaration(ExplicitSection section, Optional<IntegralValue> value) implements ZAssemblerFileEntry
{}
