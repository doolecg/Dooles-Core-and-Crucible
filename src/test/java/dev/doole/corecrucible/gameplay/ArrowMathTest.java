package dev.doole.corecrucible.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// Tests for the arrow armor-pierce maths.
class ArrowMathTest {

    @Test
    void noPierceKeepsArmorReduction() {
        assertEquals(6.0F, ArrowMath.pierced(10.0F, 6.0F, 0.0), 1e-6);
    }

    @Test
    void pierceRestoresShareOfReducedDamage() {
        // 40% of 10 ignores armor: 4 + 60% of 10 reduced by 40% (3.6) = 7.6.
        assertEquals(7.6F, ArrowMath.pierced(10.0F, 6.0F, 0.4), 1e-5);
        assertEquals(10.0F, ArrowMath.pierced(10.0F, 6.0F, 1.0), 1e-6);
    }

    @Test
    void pierceIsClampedAndNeverLowersDamage() {
        assertEquals(10.0F, ArrowMath.pierced(10.0F, 6.0F, 2.0), 1e-6);
        assertEquals(6.0F, ArrowMath.pierced(10.0F, 6.0F, -1.0), 1e-6);
        assertEquals(10.0F, ArrowMath.pierced(10.0F, 10.0F, 0.4), 1e-6);
    }
}
