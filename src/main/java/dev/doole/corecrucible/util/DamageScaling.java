package dev.doole.corecrucible.util;

/** Carries durability damage across an upgrade so the item keeps the same worn fraction. */
public final class DamageScaling {

    private DamageScaling() {
    }

    /**
     * {@code floor(oldDamage × newMax / oldMax)}, clamped to {@code [0, newMax - 1]} so an
     * upgrade never produces a broken item.
     */
    public static int scale(int oldDamage, int oldMax, int newMax) {
        if (oldDamage <= 0 || oldMax <= 0 || newMax <= 0) return 0;
        long scaled = (long) oldDamage * newMax / oldMax;
        return (int) Math.min(scaled, newMax - 1L);
    }
}
