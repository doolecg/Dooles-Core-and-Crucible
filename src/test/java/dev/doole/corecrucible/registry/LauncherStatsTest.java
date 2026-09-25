package dev.doole.corecrucible.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Tests for the bow and crossbow table and the draw-speed maths.
class LauncherStatsTest {

    @Test
    void tableMatchesDesign() {
        assertEquals(1.0, LauncherStats.of(ModTiers.WOOD).damageMultiplier(), 1e-9);
        assertEquals(2.0, LauncherStats.of(ModTiers.REINFORCED).damageMultiplier(), 1e-9);
        assertEquals(0.40, LauncherStats.of(ModTiers.REINFORCED).armorPierce(), 1e-9);
        assertEquals(465, LauncherStats.of(ModTiers.WOOD).durability(EquipType.CROSSBOW));
        assertEquals(600, LauncherStats.of(ModTiers.IRON).durability(EquipType.BOW));
        for (ModTiers tier : ModTiers.values()) assertEquals(tier, LauncherStats.of(tier).tier());
    }

    @Test
    void drawSpeedFollowsTier() {
        assertEquals(1.0, LauncherStats.drawSpeed(LauncherStats.WOOD), 1e-9);
        assertEquals(20.0 / 13.0, LauncherStats.drawSpeed(LauncherStats.REINFORCED), 1e-9);
        assertEquals(1.0, LauncherStats.drawSpeed((LauncherStats) null), 1e-9);
    }

    @Test
    void fullDrawAtTheTierDrawTime() {
        for (LauncherStats stats : LauncherStats.values()) {
            double speed = LauncherStats.drawSpeed(stats);
            assertEquals(20, LauncherStats.scaledHeldTicks(stats.drawTicks(), speed), stats.name());
            assertTrue(LauncherStats.scaledHeldTicks(stats.drawTicks() - 1, speed) < 20, stats.name());
        }
    }

    @Test
    void crossbowChargeScales() {
        assertEquals(25, LauncherStats.scaledChargeTicks(25, 1.0));
        assertEquals(16, LauncherStats.scaledChargeTicks(25, LauncherStats.drawSpeed(LauncherStats.REINFORCED)));
        assertEquals(1, LauncherStats.scaledChargeTicks(0, 1.5));
    }
}
