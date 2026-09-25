package dev.doole.corecrucible.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.doole.corecrucible.gameplay.ProgressionGraph.Step;
import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.List;
import org.junit.jupiter.api.Test;

// Tests for which tier each piece of gear upgrades to.
class ProgressionGraphTest {

    @Test
    void tiersBelowCopperAreNotUpgraded() {
        assertTrue(ProgressionGraph.next(ModTiers.WOOD, EquipType.PICKAXE).isEmpty());
        assertTrue(ProgressionGraph.next(ModTiers.WOOD, EquipType.MACE).isEmpty());
        assertTrue(ProgressionGraph.next(ModTiers.BONE, EquipType.SWORD).isEmpty());
        assertTrue(ProgressionGraph.next(ModTiers.FLINT, EquipType.SPEAR).isEmpty());
    }

    @Test
    void copperAndAboveAreSmithed() {
        assertEquals(List.of(new Step(ModTiers.IRON)), ProgressionGraph.next(ModTiers.COPPER, EquipType.HELMET));
        assertEquals(List.of(new Step(ModTiers.REINFORCED)), ProgressionGraph.next(ModTiers.NETHERITE, EquipType.MACE));
    }

    @Test
    void craftedTiers() {
        assertTrue(ProgressionGraph.isCrafted(ModTiers.FLINT));
        assertTrue(ProgressionGraph.isCrafted(ModTiers.COPPER));
        assertFalse(ProgressionGraph.isCrafted(ModTiers.IRON));
    }

    @Test
    void chainEndsAtReinforcedAndOffChainSlots() {
        assertTrue(ProgressionGraph.next(ModTiers.REINFORCED, EquipType.PICKAXE).isEmpty());
        assertTrue(ProgressionGraph.next(ModTiers.BONE, EquipType.SHOVEL).isEmpty());
        assertTrue(ProgressionGraph.next(ModTiers.WOOD, EquipType.BOOTS).isEmpty());
    }

    @Test
    void shearsStopAtIron() {
        assertEquals(List.of(new Step(ModTiers.IRON)), ProgressionGraph.next(ModTiers.COPPER, EquipType.SHEARS));
        assertTrue(ProgressionGraph.next(ModTiers.IRON, EquipType.SHEARS).isEmpty());
    }
}
