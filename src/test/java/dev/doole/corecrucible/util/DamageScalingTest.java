package dev.doole.corecrucible.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// Tests for carrying wear over when gear is upgraded.
class DamageScalingTest {

    @Test
    void undamagedStaysUndamaged() {
        assertEquals(0, DamageScaling.scale(0, 300, 600));
    }

    @Test
    void keepsWornFraction() {
        // Copper 300 -> Iron 600: half worn stays half worn
        assertEquals(300, DamageScaling.scale(150, 300, 600));
        // Iron 600 -> Emerald 1050
        assertEquals(525, DamageScaling.scale(300, 600, 1050));
    }

    @Test
    void floorsFractions() {
        // 1 * 1050 / 600 = 1.75 -> 1
        assertEquals(1, DamageScaling.scale(1, 600, 1050));
        // 7 * 150 / 60 = 17.5 -> 17
        assertEquals(17, DamageScaling.scale(7, 60, 150));
    }

    @Test
    void neverBreaksTheItem() {
        assertEquals(4499, DamageScaling.scale(3300, 3300, 4500));
        assertEquals(4499, DamageScaling.scale(5000, 3300, 4500));
    }

    @Test
    void scalingDownWorks() {
        assertEquals(150, DamageScaling.scale(300, 600, 300));
    }

    @Test
    void invalidInputsGiveZero() {
        assertEquals(0, DamageScaling.scale(-5, 300, 600));
        assertEquals(0, DamageScaling.scale(10, 0, 600));
        assertEquals(0, DamageScaling.scale(10, 300, 0));
    }

    @Test
    void noOverflowOnLargeValues() {
        assertEquals(1_999_999_999, DamageScaling.scale(1_999_999_999, 2_000_000_000, 2_000_000_000));
    }
}
