package dev.doole.corecrucible.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModRecipes;
import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

/**
 * Alloy ingots: the tier's upgrade template, the previous tier's ingot and the
 * tier's raw resource give one alloy ingot. The template is used up.
 * <p>JSON: {@code {"type": "dooles_core_crucible:alloy_smithing", "tier": "emerald",
 * "base": "dooles_core_crucible:iron_alloy_ingot", "addition": "minecraft:emerald"}}
 */
public class AlloySmithingRecipe extends ModSmithingRecipe {

    // Reads the recipe JSON (see the example above). validate() rejects a tier that has no alloy ingot.
    public static final MapCodec<AlloySmithingRecipe> CODEC = RecordCodecBuilder.<AlloySmithingRecipe>mapCodec(i -> i.group(
            ModRecipes.enumCodec(ModTiers.class).fieldOf("tier").forGetter(r -> r.tier),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("base").forGetter(r -> r.baseItem),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("addition").forGetter(r -> r.additionItem)
    ).apply(i, AlloySmithingRecipe::new)).validate(r -> ModItems.alloyIngot(r.tier) != null
            ? DataResult.success(r)
            : DataResult.error(() -> "No " + r.tier + " alloy ingot"));

    // Sends the recipe from server to client (same three fields as the JSON).
    public static final StreamCodec<RegistryFriendlyByteBuf, AlloySmithingRecipe> STREAM_CODEC = StreamCodec.composite(
            ModRecipes.enumStreamCodec(ModTiers.class), r -> r.tier,
            ByteBufCodecs.registry(Registries.ITEM), r -> r.baseItem,
            ByteBufCodecs.registry(Registries.ITEM), r -> r.additionItem,
            AlloySmithingRecipe::new);

    private final ModTiers tier;
    private final Item baseItem;
    private final Item additionItem;
    // The three ingredients, built the first time they are asked for and then reused.
    private Ingredient template;
    private Ingredient base;
    private Ingredient addition;

    public AlloySmithingRecipe(ModTiers tier, Item baseItem, Item additionItem) {
        this.tier = tier;
        this.baseItem = baseItem;
        this.additionItem = additionItem;
    }

    public ModTiers tier() {
        return tier;
    }

    @Override
    public BookTab bookTab() {
        return BookTab.ALLOYS;
    }

    @Override
    protected Ingredient template() {
        if (template == null) template = Ingredient.of(ModItems.upgradeTemplate(tier));
        return template;
    }

    @Override
    protected Ingredient base() {
        if (base == null) base = Ingredient.of(baseItem);
        return base;
    }

    @Override
    protected Ingredient addition() {
        if (addition == null) addition = Ingredient.of(additionItem);
        return addition;
    }

    @Override
    protected ItemStack result(SmithingRecipeInput input) {
        return new ItemStack(ModItems.alloyIngot(tier));
    }

    @Override
    public ItemStack resultPreview() {
        return new ItemStack(ModItems.alloyIngot(tier));
    }

    @Override
    public RecipeSerializer<AlloySmithingRecipe> getSerializer() {
        return ModRecipes.ALLOY_SMITHING;
    }
}
