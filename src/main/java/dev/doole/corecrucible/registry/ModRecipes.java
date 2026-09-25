package dev.doole.corecrucible.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.recipe.AlloySmithingRecipe;
import dev.doole.corecrucible.recipe.ProgressionSmithingRecipe;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
//? if >=1.21.2 {
import net.minecraft.core.registries.BuiltInRegistries;
//?}

/**
 * Recipe serializers. Both recipe types use vanilla's
 * smithing {@code RecipeType}. Their JSON names tiers, slots and item ids rather than
 * ingredients, so one data set loads on every version.
 */
public final class ModRecipes {

    // The two custom recipe types. A serializer tells Minecraft how to read a recipe from JSON (CODEC) and how to send
    // it to the client (STREAM_CODEC).
    public static final RecipeSerializer<ProgressionSmithingRecipe> PROGRESSION_SMITHING =
            serializer(ProgressionSmithingRecipe.CODEC, ProgressionSmithingRecipe.STREAM_CODEC);
    public static final RecipeSerializer<AlloySmithingRecipe> ALLOY_SMITHING =
            serializer(AlloySmithingRecipe.CODEC, AlloySmithingRecipe.STREAM_CODEC);

    private ModRecipes() {
    }

    /** Called by the loader-specific registration hook. */
    public static void register(BiConsumer<Identifier, RecipeSerializer<?>> registrar) {
        registrar.accept(CoreCrucible.id("progression_smithing"), PROGRESSION_SMITHING);
        registrar.accept(CoreCrucible.id("alloy_smithing"), ALLOY_SMITHING);
    }

    private static <T extends Recipe<?>> RecipeSerializer<T> serializer(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        //? if >=1.21.2 {
        return new RecipeSerializer<>(codec, streamCodec);
        //?} else {
        /*return new RecipeSerializer<>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        };
        *///?}
    }

    /** An ingredient matching an item tag. Call it lazily, once tags are bound. */
    public static Ingredient tagIngredient(TagKey<Item> tag) {
        //? if >=1.21.2 {
        return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(tag));
        //?} else {
        /*return Ingredient.of(tag);
        *///?}
    }

    /** Lower-case name codec for a mod enum, e.g. {@code "copper"} or {@code "pickaxe"}. */
    public static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
            }
        }, e -> e.name().toLowerCase(Locale.ROOT));
    }

    /** Sends a mod enum over the network as its ordinal number instead of its name. */
    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumStreamCodec(Class<E> type) {
        E[] values = type.getEnumConstants();
        return ByteBufCodecs.VAR_INT.map(i -> values[i], Enum::ordinal);
    }
}
