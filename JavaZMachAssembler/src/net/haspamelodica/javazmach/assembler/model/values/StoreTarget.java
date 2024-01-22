package net.haspamelodica.javazmach.assembler.model.values;

public sealed interface StoreTarget permits Variable, LabelReferenceVariableOnly, MacroParam
{}
