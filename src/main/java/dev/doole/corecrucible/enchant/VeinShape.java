package dev.doole.corecrucible.enchant;

import dev.doole.corecrucible.CoreCrucible;
import java.util.ArrayList;
import java.util.List;

/**
 * The patterns Vein Resonance can mine, in menu order. Level I has the first two, level II three and level III all
 * seven. Ported from Applied-Enchantments.
 */
public enum VeinShape {
    /** Every connected block of the same kind (or the same ore / log family), diagonals included. */
    // Each shape: its lang name and the Vein Resonance level needed to use it.
    SHAPELESS("shapeless", 1),
    /** A 1x1 tunnel straight into the mined face. */
    SMALL_TUNNEL("small_tunnel", 1),
    /** The 3x3 square around the mined block, facing the player. */
    SMALL_SQUARE("small_square", 2),
    /** A 3x3 tunnel straight into the mined face. */
    LARGE_TUNNEL("large_tunnel", 3),
    /** A 1 wide, 2 tall tunnel a player can walk through. */
    MINING_TUNNEL("mining_tunnel", 3),
    /** A staircase climbing up and away from the player. */
    ESCAPE_TUNNEL("escape_tunnel", 3),
    /** A staircase stepping down and away from the player. */
    MINESHAFT("mineshaft", 3);

    private final String name;
    private final int requiredLevel;

    VeinShape(String name, int requiredLevel) {
        this.name = name;
        this.requiredLevel = requiredLevel;
    }

    public String translationKey() {
        return "vein_shape." + CoreCrucible.MOD_ID + "." + name;
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    public boolean isUnlocked(int enchantmentLevel) {
        return enchantmentLevel >= requiredLevel;
    }

    /** Shapes a tool with {@code enchantmentLevel} can use, in menu order. */
    public static List<VeinShape> unlocked(int enchantmentLevel) {
        List<VeinShape> shapes = new ArrayList<>();
        for (VeinShape shape : values()) {
            if (shape.isUnlocked(enchantmentLevel)) shapes.add(shape);
        }
        return shapes;
    }

    /** The shape at {@code index}, falling back to Shapeless when it's out of range or still locked. */
    public static VeinShape effective(int index, int enchantmentLevel) {
        VeinShape[] shapes = values();
        VeinShape shape = index >= 0 && index < shapes.length ? shapes[index] : SHAPELESS;
        return shape.isUnlocked(enchantmentLevel) ? shape : SHAPELESS;
    }
}
