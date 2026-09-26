package dev.doole.corecrucible.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModRecipes;
import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

/**
 * Copper → Iron and above: the tier's upgrade template, the previous tier's piece
 * and the tier's alloy ingot. The template is used up; the piece keeps its name,
 * enchantments and trim, and comes out fully repaired ({@link UpgradeLogic}).
 * <p>JSON: {@code {"type": "dooles_core_crucible:progression_smithing", "slot": "pickaxe", "tier": "emerald"}}
 */
public class ProgressionSmithingRecipe extends ModSmithingRecipe {

    // Reads the recipe JSON (see the example above); validate() rejects paths that don't exist.
    public static final MapCodec<ProgressionSmithingRecipe> CODEC = RecordCodecBuilder.<ProgressionSmithingRecipe>mapCodec(i -> i.group(
            ModRecipes.enumCodec(EquipType.class).fieldOf("slot").forGetter(r -> r.slot),
            ModRecipes.enumCodec(ModTiers.class).fieldOf("tier").forGetter(r -> r.tier)
    ).apply(i, ProgressionSmithingRecipe::new)).validate(ProgressionSmithingRecipe::validate);

    // Sends the recipe from server to client.
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionSmithingRecipe> STREAM_CODEC = StreamCodec.composite(
            ModRecipes.enumStreamCodec(EquipType.class), r -> r.slot,
            ModRecipes.enumStreamCodec(ModTiers.class), r -> r.tier,
            ProgressionSmithingRecipe::new);

    private final EquipType slot;
    private final ModTiers tier;
    // The three ingredients, built the first time they are asked for and then reused.
    private Ingredient template;
    private Ingredient base;
    private Ingredient addition;

    public ProgressionSmithingRecipe(EquipType slot, ModTiers tier) {
        this.slot = slot;
        this.tier = tier;
    }

    private static DataResult<ProgressionSmithingRecipe> validate(ProgressionSmithingRecipe r) {
        if (r.tier.ordinal() < ModTiers.IRON.ordinal()) {
            return DataResult.error(() -> "Smithing upgrades start at iron, got " + r.tier);
        }
        if (!r.slot.existsFor(r.tier) || !r.slot.existsFor(r.previous())) {
            return DataResult.error(() -> "No " + r.tier + " " + r.slot + " upgrade path");
        }
        return DataResult.success(r);
    }

    // The tier being upgraded from (the one before this recipe's tier).
    private ModTiers previous() {
        return ModTiers.values()[tier.ordinal() - 1];
    }

    public ModTiers tier() {
        return tier;
    }

    @Override
    public BookTab bookTab() {
        return BookTab.GEAR;
    }

    @Override
    protected Ingredient template() {
        if (template == null) template = Ingredient.of(ModItems.upgradeTemplate(tier));
        return template;
    }

    @Override
    protected Ingredient base() {
        if (base == null) base = Ingredient.of(ModItems.equipment(previous(), slot));
        return base;
    }

    @Override
    protected Ingredient addition() {
        if (addition == null) addition = Ingredient.of(ModItems.alloyIngot(tier));
        return addition;
    }

    @Override
    // The actual output: the new tier's item, carrying over the old piece's name, enchantments etc.
    protected ItemStack result(SmithingRecipeInput input) {
        return UpgradeLogic.upgrade(input.base(), ModItems.equipment(tier, slot));
    }

    @Override
    // A plain item for the recipe book and JEI to show.
    public ItemStack resultPreview() {
        return new ItemStack(ModItems.equipment(tier, slot));
    }

    @Override
    public RecipeSerializer<ProgressionSmithingRecipe> getSerializer() {
        return ModRecipes.PROGRESSION_SMITHING;
    }
}
