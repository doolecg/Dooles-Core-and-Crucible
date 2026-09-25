package dev.doole.corecrucible.platform.fabric;

//? fabric {

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.config.BalanceConfig;
import dev.doole.corecrucible.enchant.VeinMining;
import dev.doole.corecrucible.gameplay.TemplateLoot;
import dev.doole.corecrucible.inventory.SmithingRecipePlacer;
import dev.doole.corecrucible.platform.Events;
import dev.doole.corecrucible.platform.MultiLoaderUtil;
import dev.doole.corecrucible.registry.ModCreativeTab;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModRecipes;
import dev.doole.corecrucible.registry.VanillaOverrides;
//? if >=1.21.2 {
import dev.doole.corecrucible.registry.ModRecipeBookCategories;
//?}
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
//? if <1.21.2 {
/*import dev.doole.corecrucible.registry.ModArmorMaterials;
*///?}

// Fabric's main startup hook: registers all of the mod's items, recipes etc. with Fabric's registries, wires up
// Fabric's events to the shared Events class, and hands off to the mod's common setup.
@Entrypoint("main")
public class FabricEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        // Before anything is registered: the balance config changes values baked into items.
        BalanceConfig.load(MultiLoaderUtil.INSTANCE.configDir());
        //? if <1.21.2 {
        /*ModArmorMaterials.register((id, material) -> Registry.register(BuiltInRegistries.ARMOR_MATERIAL, id, material));
        *///?}
        ModItems.register((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        ModCreativeTab.register((id, tab) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab));
        ModRecipes.register((id, serializer) -> Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, serializer));
        //? if >=1.21.2 {
        ModRecipeBookCategories.register((id, category) -> Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, id, category));
        //?}

        // Give every vanilla item that joins the chain its tier's durability/repair defaults.
        DefaultItemComponentEvents.MODIFY.register(context -> VanillaOverrides.forEach((item, tier, armor) ->
                //? if >=1.21.2 {
                context.modify(item, (builder, lookup, it) -> VanillaOverrides.apply(tier, armor, sink(builder), lookup))
                //?} else {
                /*context.modify(item, builder -> VanillaOverrides.apply(tier, armor, sink(builder), null))
                *///?}
        ));

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) ->
                Events.livingDamaged(entity, source, damageTaken, blocked));
        ServerLifecycleEvents.SERVER_STOPPING.register(Events::serverStopping);
        // Adds the Iron template pool to each vanilla chest TemplateLoot targets.
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (source.isBuiltin() && TemplateLoot.isTarget(key)) builder.withPool(TemplateLoot.pool(key));
        });

        // Register the two client-to-server network messages (vein-mining mode, smithing recipe book click)
        // and hook their handlers up to the shared Events class.
        //? if >=1.21.2 {
        PayloadTypeRegistry.serverboundPlay().register(VeinMining.ModePayload.TYPE, VeinMining.ModePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SmithingRecipePlacer.PlacePayload.TYPE, SmithingRecipePlacer.PlacePayload.CODEC);
        //?} else {
        /*PayloadTypeRegistry.playC2S().register(VeinMining.ModePayload.TYPE, VeinMining.ModePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SmithingRecipePlacer.PlacePayload.TYPE, SmithingRecipePlacer.PlacePayload.CODEC);
        *///?}
        ServerPlayNetworking.registerGlobalReceiver(VeinMining.ModePayload.TYPE, (payload, context) -> Events.veinMode(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(SmithingRecipePlacer.PlacePayload.TYPE,
                (payload, context) -> Events.placeSmithingRecipe(context.player(), payload));

        CoreCrucible.onInitialize();
    }

    // Wraps Fabric's default-component builder as the loader-neutral ComponentSink that VanillaOverrides expects.
    private static VanillaOverrides.ComponentSink sink(DataComponentMap.Builder builder) {
        return new VanillaOverrides.ComponentSink() {
            @Override
            public <T> void set(DataComponentType<T> type, T value) {
                builder.set(type, value);
            }
        };
    }
}
//?}
