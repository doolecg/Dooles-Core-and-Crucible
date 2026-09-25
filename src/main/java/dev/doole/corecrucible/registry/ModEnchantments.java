package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.CoreCrucible;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Keys for the 15 data-driven enchantments ported from Applied-Enchantments. Their definitions live in
 * {@code data/dooles_core_crucible/enchantment/}; behaviour lives in {@link dev.doole.corecrucible.enchant.EnchantmentHooks}.
 */
public final class ModEnchantments {
    // Each key points at a JSON file of the same name in data/dooles_core_crucible/enchantment/ (max level, which items
    // it goes on, cost). Change those files to rebalance an enchantment; what it actually does is coded in
    // enchant/EnchantmentHooks and enchant/VeinMining.
    // Mining & exploration
    // Hold the Vein Resonance key while mining to break a whole group of matching blocks in a chosen shape.
    public static final ResourceKey<Enchantment> VEIN_RESONANCE = key("vein_resonance");
    // In the dark: a tool leaves a light where it broke a block; armor puts a light at your feet (costs durability).
    public static final ResourceKey<Enchantment> LUMINOUS_WARD = key("luminous_ward");
    // Taking damage in the dark makes nearby ores glow for you for 6 seconds (8 blocks per level).
    public static final ResourceKey<Enchantment> CAVERNOUS_ECHO = key("cavernous_echo");
    // Mining gives up to one extra item per level for drops in the geode_cracker_drops tag.
    public static final ResourceKey<Enchantment> GEODE_CRACKER = key("geode_cracker");
    // Breaking a block also breaks the sand, gravel etc. stacked above it.
    public static final ResourceKey<Enchantment> TECTONIC_PULSE = key("tectonic_pulse");
    // Combat & synergy
    // While you're on fire, in lava or on magma, the enchanted gear repairs 2 durability per level every second.
    public static final ResourceKey<Enchantment> THERMAL_TEMPERING = key("thermal_tempering");
    // +10% melee damage for each hit in a row on the same target (within 3 seconds), up to +30/40/50%.
    public static final ResourceKey<Enchantment> KINETIC_RESONANCE = key("kinetic_resonance");
    // The weapon takes no durability damage; each hit costs you 2 XP instead (or a little health if you have none).
    public static final ResourceKey<Enchantment> SOUL_SYPHON = key("soul_syphon");
    // Fully drawn arrows explode where they land or hit. The explosion doesn't break blocks.
    public static final ResourceKey<Enchantment> VOLATILE_PAYLOAD = key("volatile_payload");
    // Drops from blocks you mine and mobs you kill go straight into your inventory.
    public static final ResourceKey<Enchantment> ENDERS_GRASP = key("enders_grasp");
    // Defense & utility
    // 15% chance per level to send projectiles back; always, while blocking with an Aegis Reflection shield.
    public static final ResourceKey<Enchantment> AEGIS_REFLECTION = key("aegis_reflection");
    // Boots: lava under you turns to magma for a few seconds so you can walk on it; no fire damage on magma.
    public static final ResourceKey<Enchantment> MOLTEN_TREAD = key("molten_tread");
    // Anything you hit can't teleport for 10 seconds (ender pearls, endermen, chorus fruit).
    public static final ResourceKey<Enchantment> SOUL_TETHER = key("soul_tether");
    // No upward knockback and no Levitation.
    public static final ResourceKey<Enchantment> GRAVITIC_ANCHOR = key("gravitic_anchor");
    // Blocking a hit with this shield protects nearby friends and pets for 3 seconds per level; hits on them are
    // stopped and cost the shield durability instead.
    public static final ResourceKey<Enchantment> PHALANX_WARD = key("phalanx_ward");

    public static final List<ResourceKey<Enchantment>> ALL = List.of(
            VEIN_RESONANCE, LUMINOUS_WARD, CAVERNOUS_ECHO, GEODE_CRACKER, TECTONIC_PULSE,
            THERMAL_TEMPERING, KINETIC_RESONANCE, SOUL_SYPHON, VOLATILE_PAYLOAD, ENDERS_GRASP,
            AEGIS_REFLECTION, MOLTEN_TREAD, SOUL_TETHER, GRAVITIC_ANCHOR, PHALANX_WARD);

    private ModEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, CoreCrucible.id(path));
    }
}
