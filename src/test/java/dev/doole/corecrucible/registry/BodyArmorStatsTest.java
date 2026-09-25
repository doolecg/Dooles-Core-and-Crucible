package dev.doole.corecrucible.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Tests that the horse and wolf armor table matches vanilla where it should and gets better every tier.
class BodyArmorStatsTest {

    @Test
    void vanillaTiersMatchVanilla() {
        // Vanilla horse armor keeps its own armor points; the table must agree with it.
        assertEquals(4, BodyArmorStats.of(ModTiers.COPPER).horseDefense());
        assertEquals(5, BodyArmorStats.of(ModTiers.IRON).horseDefense());
        assertEquals(11, BodyArmorStats.of(ModTiers.DIAMOND).horseDefense());
        assertEquals(19, BodyArmorStats.of(ModTiers.NETHERITE).horseDefense());
        // Copper wolf armor is vanilla's armadillo scute armor: 4 × 16 durability.
        assertEquals(64, BodyArmorStats.of(ModTiers.COPPER).wolfDurability());
    }

    @Test
    void climbsEveryTier() {
        BodyArmorStats[] rows = BodyArmorStats.values();
        for (int i = 1; i < rows.length; i++) {
            assertTrue(rows[i].horseDefense() > rows[i - 1].horseDefense(), rows[i] + " horse defense");
            assertTrue(rows[i].wolfDurability() > rows[i - 1].wolfDurability(), rows[i] + " wolf durability");
        }
    }

    @Test
    void noBodyArmorBelowCopper() {
        assertNull(BodyArmorStats.of(ModTiers.WOOD));
        assertNull(BodyArmorStats.of(ModTiers.BONE));
        assertNull(BodyArmorStats.of(ModTiers.FLINT));
    }
}
