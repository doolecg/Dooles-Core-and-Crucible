package dev.doole.corecrucible.gameplay;

import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.List;

/**
 * The upgrade path of every piece of gear (which tier comes next), shared by the tooltip, the JEI category and the smithing recipe
 * book. Pure logic over {@link ModTiers} and {@link EquipType}; recipes themselves are data.
 *
 * <p>Only Copper → Iron and above are upgrades (smithing table). Wood, Bone, Flint and Copper pieces are crafted
 * new in a crafting table, so tiers below Copper have no next step.
 */
public final class ProgressionGraph {

    /** One smithing upgrade: the target tier. It takes that tier's template and alloy ingot (and maybe a catalyst). */
    public record Step(ModTiers to) {
    }

    private ProgressionGraph() {
    }

    /** True for the tiers that are crafted new rather than upgraded into (Wood, Bone, Flint, Copper). */
    public static boolean isCrafted(ModTiers tier) {
        return tier.ordinal() <= ModTiers.COPPER.ordinal();
    }

    /**
     * The upgrade out of {@code from} for this slot; empty below Copper, at the top of the chain, or off it.
     * Also empty when the next tier doesn't have this slot at all (shears stop at Iron).
     */
    public static List<Step> next(ModTiers from, EquipType type) {
        if (!type.existsFor(from) || from.ordinal() < ModTiers.COPPER.ordinal() || from == ModTiers.REINFORCED) {
            return List.of();
        }
        ModTiers to = ModTiers.values()[from.ordinal() + 1];
        return type.existsFor(to) ? List.of(new Step(to)) : List.of();
    }
}
