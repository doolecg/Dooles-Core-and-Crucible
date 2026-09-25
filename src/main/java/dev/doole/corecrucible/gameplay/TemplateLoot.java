package dev.doole.corecrucible.gameplay;

import dev.doole.corecrucible.config.BalanceConfig;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

/**
 * The Iron Upgrade Template, the first of the template chain, is structure loot, the way the Netherite
 * template is bastion loot. Every later template is crafted from the one below it. The loaders add {@link #pool} to
 * each vanilla chest in {@link #CHESTS}: Fabric in {@code LootTableEvents.MODIFY}, NeoForge in
 * {@code LootTableLoadEvent}.
 */
public final class TemplateLoot {

    /** One chest the Iron template can turn up in: its config name, loot table and default chance per chest. */
    private record Chest(String name, ResourceKey<LootTable> table, float chance) {
    }

    /** Village smiths and dungeons are the likeliest places. */
    // To add a chest, add a line here: a config name, the vanilla loot table and a default chance (0.5 = half of those
    // chests). Players can change each chance in the config file under template_loot.
    private static final List<Chest> CHESTS = List.of(
            new Chest("simple_dungeon", BuiltInLootTables.SIMPLE_DUNGEON, 0.5f),
            new Chest("village_weaponsmith", BuiltInLootTables.VILLAGE_WEAPONSMITH, 0.4f),
            new Chest("village_toolsmith", BuiltInLootTables.VILLAGE_TOOLSMITH, 0.4f),
            new Chest("village_armorer", BuiltInLootTables.VILLAGE_ARMORER, 0.4f),
            new Chest("village_plains_house", BuiltInLootTables.VILLAGE_PLAINS_HOUSE, 0.1f),
            new Chest("village_taiga_house", BuiltInLootTables.VILLAGE_TAIGA_HOUSE, 0.1f),
            new Chest("village_savanna_house", BuiltInLootTables.VILLAGE_SAVANNA_HOUSE, 0.1f),
            new Chest("village_snowy_house", BuiltInLootTables.VILLAGE_SNOWY_HOUSE, 0.1f),
            new Chest("village_desert_house", BuiltInLootTables.VILLAGE_DESERT_HOUSE, 0.1f),
            new Chest("abandoned_mineshaft", BuiltInLootTables.ABANDONED_MINESHAFT, 0.2f),
            new Chest("stronghold_corridor", BuiltInLootTables.STRONGHOLD_CORRIDOR, 0.3f),
            new Chest("desert_pyramid", BuiltInLootTables.DESERT_PYRAMID, 0.25f),
            new Chest("jungle_temple", BuiltInLootTables.JUNGLE_TEMPLE, 0.3f),
            new Chest("pillager_outpost", BuiltInLootTables.PILLAGER_OUTPOST, 0.3f),
            new Chest("woodland_mansion", BuiltInLootTables.WOODLAND_MANSION, 0.3f),
            new Chest("shipwreck_supply", BuiltInLootTables.SHIPWRECK_SUPPLY, 0.15f),
            new Chest("ruined_portal", BuiltInLootTables.RUINED_PORTAL, 0.15f));

    private TemplateLoot() {
    }

    private static Chest chest(ResourceKey<LootTable> key) {
        for (Chest chest : CHESTS) {
            if (chest.table.equals(key)) return chest;
        }
        return null;
    }

    // True for the listed chests, unless the player set that chest's chance to 0 in the config.
    public static boolean isTarget(ResourceKey<LootTable> key) {
        Chest chest = chest(key);
        return chest != null && chance(chest) > 0;
    }

    /** The bonus pool for one of the {@link #CHESTS}. */
    public static LootPool.Builder pool(ResourceKey<LootTable> key) {
        return LootPool.lootPool()
                .add(LootItem.lootTableItem(ModItems.upgradeTemplate(ModTiers.IRON)))
                .when(LootItemRandomChanceCondition.randomChance(Math.min(1.0f, chance(chest(key)))));
    }

    // The chance from the config file, falling back to the default above.
    private static float chance(Chest chest) {
        return BalanceConfig.get("template_loot." + chest.name, chest.chance);
    }

    /** The {@code template_loot} section of the balance config ({@code config/dooles_core_crucible.json}). */
    public static void writeDefaults(Map<String, Double> out) {
        for (Chest chest : CHESTS) out.put("template_loot." + chest.name, (double) chest.chance);
    }
}
