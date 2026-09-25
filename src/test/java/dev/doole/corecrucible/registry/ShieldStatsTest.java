package dev.doole.corecrucible.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Tests for the shield table (wood must match vanilla, stats climb with tier).
class ShieldStatsTest {

    @Test
    void woodIsTheVanillaShield() {
        // Wood is minecraft:shield: its row must match vanilla, or the vanilla shield would change.
        ShieldStats wood = ShieldStats.of(ModTiers.WOOD);
        assertEquals(336, wood.durability());
        assertEquals(ShieldStats.VANILLA_RAISE_TICKS, wood.raiseTicks());
        assertEquals(ShieldStats.VANILLA_DISABLE_SECONDS, wood.disableSeconds(), 1e-9);
        assertEquals(0.0, wood.knockbackResistance(), 1e-9);
        assertEquals(0.0, wood.sideCover(), 1e-9);
    }

    @Test
    void tableMatchesDesign() {
        ShieldStats iron = ShieldStats.of(ModTiers.IRON);
        assertEquals(600, iron.durability());
        assertEquals(4, iron.raiseTicks());
        assertEquals(3.5, iron.disableSeconds(), 1e-9);
        ShieldStats reinforced = ShieldStats.of(ModTiers.REINFORCED);
        assertEquals(4500, reinforced.durability());
        assertEquals(1, reinforced.raiseTicks());
        assertEquals(1.0, reinforced.disableSeconds(), 1e-9);
        assertEquals(0.6, reinforced.knockbackResistance(), 1e-9);
        assertEquals(0.6, reinforced.sideCover(), 1e-9);
        for (ShieldStats stats : ShieldStats.values()) assertEquals(stats.tier(), ShieldStats.of(stats.tier()).tier());
    }

    @Test
    void everyTierIsAtLeastAsGoodAsTheOneBelow() {
        ShieldStats[] rows = ShieldStats.values();
        for (int i = 1; i < rows.length; i++) {
            ShieldStats lower = rows[i - 1];
            ShieldStats upper = rows[i];
            if (upper.tier() == ModTiers.FLINT) lower = ShieldStats.of(ModTiers.WOOD); // Bone and Flint are parallel
            assertTrue(upper.durability() >= lower.durability(), upper + " durability");
            assertTrue(upper.raiseTicks() <= lower.raiseTicks(), upper + " raise time");
            assertTrue(upper.disableSeconds() <= lower.disableSeconds(), upper + " axe disable");
            assertTrue(upper.knockbackResistance() >= lower.knockbackResistance(), upper + " knockback resistance");
            assertTrue(upper.sideCover() >= lower.sideCover(), upper + " side cover");
        }
    }
}
