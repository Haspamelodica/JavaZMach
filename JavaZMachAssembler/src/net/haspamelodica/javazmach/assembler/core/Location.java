package net.haspamelodica.javazmach.assembler.core;

public sealed interface Location permits EntryStartLocation, EntryEndLocation, SpecialLocation, Section
{}
