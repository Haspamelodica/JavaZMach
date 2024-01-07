package net.haspamelodica.javazmach.assembler.model;

import java.util.List;

public record Routine(String name, List<RoutineLocal> locals) implements ZAssemblerFileEntry
{}
