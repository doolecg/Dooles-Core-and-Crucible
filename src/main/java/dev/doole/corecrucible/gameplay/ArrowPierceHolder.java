package dev.doole.corecrucible.gameplay;

/** Armor pierce a tiered launcher gave an arrow. Implemented on {@code AbstractArrow} by a mixin. */
public interface ArrowPierceHolder {

    /** The pierce set at launch, or NaN when none was set (a vanilla shot, or an arrow loaded from disk). */
    double dooles_core_crucible$pierce();

    void dooles_core_crucible$setPierce(double pierce);
}
