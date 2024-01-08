package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record ZObjectTable(List<Property> defaultProperties, List<ZObject> objects) implements ZAssemblerFileEntry
{}
