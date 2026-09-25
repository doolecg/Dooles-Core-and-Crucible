package dev.doole.corecrucible.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// Tests for the shield side-cover angle maths.
class ShieldGuardTest {

    @Test
    void angleFromWhereTheEntityFaces() {
        // Yaw 0 faces +Z; yaw 90 faces -X (Minecraft turns clockwise seen from above).
        assertEquals(0.0, Math.toDegrees(ShieldGuard.angleOff(0.0F, 0, 5)), 1e-6);
        assertEquals(90.0, Math.toDegrees(ShieldGuard.angleOff(0.0F, 3, 0)), 1e-6);
        assertEquals(90.0, Math.toDegrees(ShieldGuard.angleOff(0.0F, -3, 0)), 1e-6);
        assertEquals(180.0, Math.toDegrees(ShieldGuard.angleOff(0.0F, 0, -2)), 1e-6);
        assertEquals(0.0, Math.toDegrees(ShieldGuard.angleOff(90.0F, -4, 0)), 1e-6);
        assertEquals(135.0, Math.toDegrees(ShieldGuard.angleOff(0.0F, 1, -1)), 1e-6);
    }

    @Test
    void sideBandIsBetweenVanillasBlockAndTheBack() {
        // Vanilla blocks up to 90° off the front; side cover reaches 135°, leaving the back quarter open.
        assertEquals(135.0, Math.toDegrees(ShieldGuard.SIDE_ANGLE), 1e-9);
    }
}
