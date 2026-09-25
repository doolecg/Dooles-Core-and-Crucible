package dev.doole.corecrucible.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// Tests for the fishing rod table (wood must match vanilla, stats climb with tier).
class RodStatsTest {

    @Test
    void tableMatchesDesign() {
        assertEquals(64, RodStats.of(ModTiers.WOOD).durability());
        assertEquals(0, RodStats.of(ModTiers.WOOD).lure());
        assertEquals(0, RodStats.of(ModTiers.WOOD).luck());
        assertEquals(1.0, RodStats.of(ModTiers.WOOD).reel(), 1e-9);
        assertEquals(1.0, RodStats.of(ModTiers.WOOD).cast(), 1e-9);

        assertEquals(96, RodStats.of(ModTiers.BONE).durability());
        assertEquals(96, RodStats.of(ModTiers.FLINT).durability());
        assertEquals(1.05, RodStats.of(ModTiers.FLINT).reel(), 1e-9);

        assertEquals(1, RodStats.of(ModTiers.IRON).lure());
        assertEquals(0, RodStats.of(ModTiers.IRON).luck());

        assertEquals(320, RodStats.of(ModTiers.DIAMOND).durability());
        assertEquals(1.3, RodStats.of(ModTiers.DIAMOND).reel(), 1e-9);
        assertEquals(1.25, RodStats.of(ModTiers.DIAMOND).cast(), 1e-9);

        assertEquals(512, RodStats.of(ModTiers.REINFORCED).durability());
        assertEquals(3, RodStats.of(ModTiers.REINFORCED).lure());
        assertEquals(2, RodStats.of(ModTiers.REINFORCED).luck());
        assertEquals(1.6, RodStats.of(ModTiers.REINFORCED).reel(), 1e-9);
        assertEquals(1.4, RodStats.of(ModTiers.REINFORCED).cast(), 1e-9);

        for (ModTiers tier : ModTiers.values()) assertEquals(tier, RodStats.of(tier).tier());
    }
}
