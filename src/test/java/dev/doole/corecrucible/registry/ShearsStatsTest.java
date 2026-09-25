package dev.doole.corecrucible.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

// Tests for the shears table.
class ShearsStatsTest {

    @Test
    void tableMatchesDesign() {
        assertEquals(120, ShearsStats.of(ModTiers.BONE).durability());
        assertEquals(0.6, ShearsStats.of(ModTiers.BONE).speedMultiplier(), 1e-9);

        assertEquals(120, ShearsStats.of(ModTiers.FLINT).durability());
        assertEquals(0.6, ShearsStats.of(ModTiers.FLINT).speedMultiplier(), 1e-9);

        assertEquals(180, ShearsStats.of(ModTiers.COPPER).durability());
        assertEquals(0.8, ShearsStats.of(ModTiers.COPPER).speedMultiplier(), 1e-9);

        for (ShearsStats stats : ShearsStats.values()) assertEquals(stats.tier(), ShearsStats.of(stats.tier()).tier());
    }

    @Test
    void ironHasNoRow() {
        // Iron is the vanilla pair of shears: it keeps vanilla stats, so it has no ShearsStats row.
        assertNull(ShearsStats.of(ModTiers.IRON));
        assertNull(ShearsStats.of(ModTiers.WOOD));
        assertNull(ShearsStats.of(ModTiers.REINFORCED));
    }
}
