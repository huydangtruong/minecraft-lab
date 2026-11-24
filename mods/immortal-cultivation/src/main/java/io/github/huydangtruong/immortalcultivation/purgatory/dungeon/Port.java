package io.github.huydangtruong.immortalcultivation.purgatory.dungeon;

/** A doorway on the side of a room; slot = which cell along that side (0 for a one-cell room). */
public record Port(Dir side, int slot) {}
