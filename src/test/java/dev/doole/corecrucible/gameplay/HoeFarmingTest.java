package dev.doole.corecrucible.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.doole.corecrucible.gameplay.HoeFarming.Area;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.List;
import org.junit.jupiter.api.Test;

// Tests for the hoe till area, harvest bonus and rounding.
class HoeFarmingTest {

    @Test
    void areaGrowsWithTier() {
        assertEquals(Area.SINGLE, HoeFarming.area(ModTiers.COPPER));
        assertEquals(Area.ROW, HoeFarming.area(ModTiers.IRON));
        assertEquals(Area.ROW, HoeFarming.area(ModTiers.DIAMOND));
        assertEquals(Area.SQUARE, HoeFarming.area(ModTiers.OBSIDIAN));
        assertEquals(Area.SQUARE, HoeFarming.area(ModTiers.REINFORCED));
    }

    @Test
    void rowRunsSideways() {
        // Facing north, the player's right is east (+x): the row covers x-1 and x+1.
        List<int[]> row = HoeFarming.extraOffsets(Area.ROW, 1, 0);
        assertEquals(2, row.size());
        assertEquals(-1, row.get(0)[0]);
        assertEquals(0, row.get(0)[1]);
        assertEquals(1, row.get(1)[0]);
        assertEquals(0, row.get(1)[1]);
    }

    @Test
    void squareCoversTheEightNeighbours() {
        List<int[]> square = HoeFarming.extraOffsets(Area.SQUARE, 0, 1);
        assertEquals(8, square.size());
        for (int[] offset : square) {
            assertFalse(offset[0] == 0 && offset[1] == 0);
        }
        assertTrue(HoeFarming.extraOffsets(Area.SINGLE, 1, 0).isEmpty());
    }

    @Test
    void bonusRoundsByChance() {
        // 3 produce × 50% = 1.5: 1, plus 1 more when the roll is under 0.5.
        assertEquals(2, HoeFarming.bonusCount(3, 0.5, 0.2));
        assertEquals(1, HoeFarming.bonusCount(3, 0.5, 0.7));
        assertEquals(0, HoeFarming.bonusCount(3, 0.0, 0.0));
        assertEquals(0, HoeFarming.bonusCount(0, 0.5, 0.0));
    }

    @Test
    void replantFromIron() {
        assertFalse(HoeFarming.replants(ModTiers.COPPER));
        assertTrue(HoeFarming.replants(ModTiers.IRON));
        assertEquals(0.5, HoeFarming.harvestBonus(ModTiers.REINFORCED));
    }

    @Test
    void roundRandomUsesFraction() {
        assertEquals(3, HoeFarming.roundRandom(2.25, 0.2));
        assertEquals(2, HoeFarming.roundRandom(2.25, 0.3));
        assertEquals(2, HoeFarming.roundRandom(2.25, 0.25));
        assertEquals(4, HoeFarming.roundRandom(4.0, 0.0));
    }
}
