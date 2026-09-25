package dev.doole.corecrucible.registry;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
//? if >=1.21.2 {
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Repairable;
//?}

/**
 * Vanilla gear that joins the chain. Tools get the tier's durability; tools and
 * armor of Iron, Diamond and Netherite repair with the tier's alloy. Mining speed is unchanged
 * because the vanilla speeds already match the tier table. Attack attributes are never touched.
 */
public final class VanillaOverrides {

    /** A loader-neutral view of a default-component builder. */
    public interface ComponentSink {
        <T> void set(DataComponentType<T> type, T value);
    }

    public interface Visitor {
        void visit(Item item, ModTiers tier, boolean armor);
    }

    private static Map<Item, ModTiers> tools;
    private static Map<Item, ModTiers> armor;

    private VanillaOverrides() {
    }

    // Calls the visitor once for every vanilla item that joins the chain: tools first, then armor.
    public static void forEach(Visitor visitor) {
        build();
        for (Map.Entry<Item, ModTiers> entry : tools.entrySet()) {
            visitor.visit(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<Item, ModTiers> entry : armor.entrySet()) {
            visitor.visit(entry.getKey(), entry.getValue(), true);
        }
    }

    /**
     * Writes the overrides for one item.
     *
     * @param lookup registry access for tag-based components; unused on 1.21.1
     */
    // Tools take the tier's durability from ModTiers (so a config change to tiers.iron.durability changes vanilla's iron
    // tools too). Armor keeps vanilla durability. On 1.21.1 the repair change is done by mixins instead.
    public static void apply(ModTiers tier, boolean isArmor, ComponentSink sink, HolderLookup.Provider lookup) {
        if (!isArmor) sink.set(DataComponents.MAX_DAMAGE, tier.durability());
        //? if >=1.21.2 {
        if (changesRepair(tier)) {
            sink.set(DataComponents.REPAIRABLE, new Repairable(lookup.lookupOrThrow(Registries.ITEM).getOrThrow(tier.repairItems())));
        }
        //?}
    }

    /** The tier whose repair tag replaces this vanilla item's repair material, or null. Used by the 1.21.1 mixins. */
    public static ModTiers repairOverride(Item item) {
        build();
        ModTiers tier = tools.get(item);
        if (tier == null) tier = armor.get(item);
        return tier != null && changesRepair(tier) ? tier : null;
    }

    /** Wood and Copper keep their vanilla repair materials (planks, copper ingot). */
    private static boolean changesRepair(ModTiers tier) {
        return tier == ModTiers.IRON || tier == ModTiers.DIAMOND || tier == ModTiers.NETHERITE;
    }

    // Works out, once, which vanilla items join the chain and remembers each one's tier.
    private static void build() {
        if (tools != null) return;
        Map<Item, ModTiers> t = new IdentityHashMap<>();
        Map<Item, ModTiers> a = new IdentityHashMap<>();
        for (ModTiers tier : ModTiers.values()) {
            for (EquipType type : EquipType.values()) {
                // The vanilla bow, crossbow, fishing rod, shield (Wood), shears (Iron), horse armor and wolf armor keep
                // their own durability and repair.
                if (!ModItems.isVanilla(tier, type) || type.isLauncher() || type.isFishingRod() || type.isShears()
                        || type.isShield() || type.isBodyArmor()) continue;
                if (type.isArmor()) {
                    a.put(ModItems.equipment(tier, type), tier);
                } else {
                    t.put(ModItems.equipment(tier, type), tier);
                }
            }
        }
        armor = a;
        tools = t;
    }
}
