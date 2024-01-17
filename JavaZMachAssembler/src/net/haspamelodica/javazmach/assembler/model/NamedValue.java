package net.haspamelodica.javazmach.assembler.model;

public record NamedValue(String name, IntegralValue value) implements ZAssemblerFileEntry
{}
