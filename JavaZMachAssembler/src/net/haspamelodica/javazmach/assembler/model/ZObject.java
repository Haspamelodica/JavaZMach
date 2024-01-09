package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record ZObject(ZString name, List<ZObjectEntry> entries) implements ZObjectEntry
{}
