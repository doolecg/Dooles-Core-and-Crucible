package dev.doole.corecrucible.gameplay;

/** Armor-piercing maths for arrows shot from a tiered bow or crossbow. Pure maths, unit-tested. */
public final class ArrowMath {

    private ArrowMath() {
    }

    /**
     * Damage after armor when {@code pierce} of it ignores armor: the pierced share keeps its full
     * value and the rest takes the normal armor reduction.
     *
     * @param raw       damage before armor
     * @param afterArmor vanilla damage after armor for {@code raw}
     * @param pierce    share that ignores armor, clamped to 0..1
     */
    public static float pierced(float raw, float afterArmor, double pierce) {
        // Example: 10 raw damage, 6 after armor, pierce 0.25 -> 6 + 0.25 * (10 - 6) = 7.
        double p = Math.max(0.0, Math.min(1.0, pierce));
        if (p == 0.0 || afterArmor >= raw) return afterArmor;
        return (float) (afterArmor + p * (raw - afterArmor));
    }
}
