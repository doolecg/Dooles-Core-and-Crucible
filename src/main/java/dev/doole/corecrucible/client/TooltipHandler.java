package dev.doole.corecrucible.client;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.gameplay.Catalysts;
import dev.doole.corecrucible.gameplay.HoeFarming;
import dev.doole.corecrucible.gameplay.ProgressionGraph;
import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.LauncherStats;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import dev.doole.corecrucible.registry.RodStats;
import dev.doole.corecrucible.registry.ShearsStats;
import dev.doole.corecrucible.registry.ShieldStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Doole's Core & Crucible tooltip. Called from Fabric's {@code ItemTooltipCallback} and NeoForge's
 * {@code ItemTooltipEvent}. Adds a block under the item name: a Shift hint, or the detailed stats while Shift is held.
 */
public final class TooltipHandler {
    private static final String KEY = "tooltip." + CoreCrucible.MOD_ID + ".";
    /** Vanilla crossbow charge time in ticks. */
    private static final int CROSSBOW_CHARGE_TICKS = 25;

    private TooltipHandler() {
    }

    // Only gear in the chain gets the extra lines. They go right under the item name: stats and the next upgrade when
    // Shift is held, otherwise a "hold Shift" hint, then the tier's flavour text at the end.
    public static void append(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty() || lines.isEmpty()) return;
        Item item = stack.getItem();
        ModTiers tier = ModItems.tierOf(item);
        EquipType type = ModItems.typeOf(item);
        if (tier == null || type == null) return;

        List<Component> block = new ArrayList<>();
        block.add(Component.empty());
        if (!ClientCompat.shiftDown()) {
            block.add(Component.translatable(KEY + "hold_shift", Component.translatable(KEY + "shift").withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            gearStats(stack, tier, type, block);
            progression(tier, type, block);
        }
        block.add(Component.empty());
        block.add(Component.translatable(KEY + "flavor." + tier.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        lines.addAll(1, block);
    }

    // ---- Stats ----------------------------------------------------------------------------------------------

    // Adds the durability, tier, and (per item type) stat lines shown while Shift is held.
    private static void gearStats(ItemStack stack, ModTiers tier, EquipType type, List<Component> out) {
        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int left = max - stack.getDamageValue();
            double ratio = (double) left / max;
            // White when undamaged; otherwise green/yellow/red as it wears down.
            ChatFormatting color;
            if (!stack.isDamaged()) {
                color = ChatFormatting.WHITE;
            } else if (ratio > 0.75) {
                color = ChatFormatting.GREEN;
            } else if (ratio >= 0.25) {
                color = ChatFormatting.YELLOW;
            } else {
                color = ChatFormatting.RED;
            }
            out.add(stat("durability", Component.literal(left + " / " + max).withStyle(color)));
        }
        // Horse and wolf armor: vanilla's own tooltip already lists their armor points.
        if (type.isArmor() || type.isBodyArmor()) {
            out.add(stat("tier", tierName(tier).withStyle(ChatFormatting.WHITE)));
            return;
        }
        if (type.isShield()) {
            shieldStats(ShieldStats.of(tier), out);
            out.add(stat("tier", tierName(tier).withStyle(ChatFormatting.WHITE)));
            return;
        }
        if (type.isLauncher()) {
            launcherStats(LauncherStats.of(tier), type, out);
            out.add(stat("tier", tierName(tier).withStyle(ChatFormatting.WHITE)));
            return;
        }
        if (type.isFishingRod()) {
            // Wood is the vanilla rod: its lure/luck/reel/cast are all neutral, so it shows nothing new here.
            if (tier != ModTiers.WOOD) rodStats(RodStats.of(tier), out);
            out.add(stat("tier", tierName(tier).withStyle(ChatFormatting.WHITE)));
            return;
        }
        if (type.isShears()) {
            // Iron is the vanilla pair of shears: it has no ShearsStats row, so it shows only its tier.
            ShearsStats stats = ShearsStats.of(tier);
            if (stats != null) out.add(stat("mining_speed", Component.literal("×" + format(stats.speedMultiplier())).withStyle(ChatFormatting.WHITE)));
            out.add(stat("tier", tierName(tier).withStyle(ChatFormatting.WHITE)));
            return;
        }
        boolean digger = type == EquipType.PICKAXE || type == EquipType.AXE || type == EquipType.SHOVEL || type == EquipType.HOE;
        if (digger) out.add(stat("mining_speed", Component.literal(format(tier.speed())).withStyle(ChatFormatting.WHITE)));
        out.add(stat("attack_damage", Component.literal("+" + format(tier.attackBonus())).withStyle(ChatFormatting.WHITE)));
        if (digger) {
            out.add(stat("harvest_level", Component.literal(tier.harvestLevel() + " (").append(tierName(tier)).append(")")
                    .withStyle(ChatFormatting.WHITE)));
        }
        if (type == EquipType.HOE) {
            out.add(stat("till_area", Component.literal(HoeFarming.area(tier).label()).withStyle(ChatFormatting.WHITE)));
            MutableComponent harvest = Component.literal("+" + Math.round(HoeFarming.harvestBonus(tier) * 100) + "%");
            if (HoeFarming.replants(tier)) harvest.append(Component.translatable(KEY + "replants"));
            out.add(stat("harvest_bonus", harvest.withStyle(ChatFormatting.WHITE)));
        }
    }

    /** Arrow damage and pierce, and the full draw (bow) or charge (crossbow, before Quick Charge) in seconds. */
    private static void launcherStats(LauncherStats stats, EquipType type, List<Component> out) {
        out.add(stat("arrow_damage", Component.literal("×" + format(stats.damageMultiplier())).withStyle(ChatFormatting.WHITE)));
        out.add(stat("armor_pierce", Component.literal(Math.round(stats.armorPierce() * 100) + "%").withStyle(ChatFormatting.WHITE)));
        boolean crossbow = type == EquipType.CROSSBOW;
        double ticks = crossbow ? CROSSBOW_CHARGE_TICKS * stats.drawTicks() / (double) LauncherStats.VANILLA_DRAW_TICKS : stats.drawTicks();
        out.add(stat(crossbow ? "charge_time" : "draw_time",
                Component.translatable(KEY + "seconds", format(Math.round(ticks / 20.0 * 100) / 100.0)).withStyle(ChatFormatting.WHITE)));
    }

    /** Lure and Luck only show when the tier adds any; Reel Strength and Cast Distance always show for chain rods. */
    private static void rodStats(RodStats stats, List<Component> out) {
        if (stats.lure() > 0) out.add(stat("lure", Component.literal("+" + stats.lure()).withStyle(ChatFormatting.WHITE)));
        if (stats.luck() > 0) out.add(stat("luck", Component.literal("+" + stats.luck()).withStyle(ChatFormatting.WHITE)));
        out.add(stat("reel_strength", Component.literal("×" + format(stats.reel())).withStyle(ChatFormatting.WHITE)));
        out.add(stat("cast_distance", Component.literal("×" + format(stats.cast())).withStyle(ChatFormatting.WHITE)));
    }

    /** Raise and axe-disable times always; knockback resistance and side cover only when the tier has any. */
    private static void shieldStats(ShieldStats stats, List<Component> out) {
        out.add(stat("raise_time", seconds(stats.raiseTicks() / 20.0)));
        out.add(stat("axe_disable", seconds(stats.disableSeconds())));
        if (stats.knockbackResistance() > 0) {
            out.add(stat("knockback_resist", Component.literal(Math.round(stats.knockbackResistance() * 100) + "%").withStyle(ChatFormatting.WHITE)));
        }
        if (stats.sideCover() > 0) {
            out.add(stat("side_cover", Component.literal(Math.round(stats.sideCover() * 100) + "%").withStyle(ChatFormatting.WHITE)));
        }
    }

    private static MutableComponent seconds(double value) {
        return Component.translatable(KEY + "seconds", format(Math.round(value * 100) / 100.0)).withStyle(ChatFormatting.WHITE);
    }

    // ---- Progression ----------------------------------------------------------------------------------------

    // The "Progression" part: the next tier and what the upgrade takes (template + alloy ingot + any catalyst).
    private static void progression(ModTiers tier, EquipType type, List<Component> out) {
        out.add(Component.empty());
        out.add(Component.translatable(KEY + "header.progression").withStyle(ChatFormatting.AQUA));
        List<ProgressionGraph.Step> steps = ProgressionGraph.next(tier, type);
        if (steps.isEmpty() && tier.ordinal() < ModTiers.COPPER.ordinal()) {
            // Below Copper nothing upgrades: the chain starts with a Copper piece crafted new.
            out.add(stat("next_tier", tierName(ModTiers.COPPER).withStyle(ChatFormatting.WHITE)));
            out.add(stat("requires", Component.translatable(KEY + "crafted_new").withStyle(ChatFormatting.WHITE)));
            return;
        }
        if (steps.isEmpty()) {
            out.add(stat("next_tier", Component.translatable(KEY + "max_level").withStyle(ChatFormatting.WHITE)));
            out.add(stat("requires", Component.translatable(KEY + "not_applicable").withStyle(ChatFormatting.WHITE)));
            return;
        }
        MutableComponent next = Component.empty();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) next.append(Component.translatable(KEY + "or"));
            next.append(tierName(steps.get(i).to()));
        }
        out.add(stat("next_tier", next.withStyle(ChatFormatting.WHITE)));
        for (ProgressionGraph.Step step : steps) {
            MutableComponent requires = name(ModItems.upgradeTemplate(step.to())).append(" + ").append(name(ModItems.alloyIngot(step.to())));
            Item catalyst = Catalysts.forUpgrade(step.to());
            if (catalyst != null) requires.append(" + ").append(name(catalyst));
            out.add(stat("requires", requires.withStyle(ChatFormatting.WHITE)));
        }
    }

    // ---- Helpers --------------------------------------------------------------------------------------------

    // One "Label: value" line. Labels are lang keys like tooltip.dooles_core_crucible.durability.
    private static MutableComponent stat(String key, Component value) {
        return Component.translatable(KEY + key).withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY)).append(value);
    }

    private static MutableComponent tierName(ModTiers tier) {
        return Component.translatable("tier." + CoreCrucible.MOD_ID + "." + tier.name().toLowerCase(Locale.ROOT));
    }

    private static MutableComponent name(Item item) {
        return item == null ? Component.literal("?") : new ItemStack(item).getHoverName().copy();
    }

    // Numbers for display: whole numbers as "2.0", others with up to two decimals.
    private static String format(double value) {
        return value == Math.rint(value) ? String.format(Locale.ROOT, "%.1f", value) : String.format(Locale.ROOT, "%.2f", value).replaceAll("0$", "");
    }
}
