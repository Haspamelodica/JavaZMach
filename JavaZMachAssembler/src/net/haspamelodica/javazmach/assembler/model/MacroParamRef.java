package net.haspamelodica.javazmach.assembler.model;

/** For some reason, JDT complains about a cycle in the type hierarchy when making {@link MacroParam} itself implement {@link IntegralValue}. */
public record MacroParamRef(MacroParam param) implements IntegralValue
{}
