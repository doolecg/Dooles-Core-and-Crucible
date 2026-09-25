package dev.doole.corecrucible.platform.neoforge;

//? neoforge {

/*import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.config.BalanceConfig;
import dev.doole.corecrucible.enchant.VeinMining;
import dev.doole.corecrucible.inventory.SmithingRecipePlacer;
import dev.doole.corecrucible.platform.Events;
import dev.doole.corecrucible.platform.MultiLoaderUtil;
import dev.doole.corecrucible.registry.ModCreativeTab;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModRecipes;
//? if >=1.21.2 {
import dev.doole.corecrucible.registry.ModRecipeBookCategories;
//?}
import dev.doole.corecrucible.registry.VanillaOverrides;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
//? if >=1.21.2 {
import net.minecraft.core.component.DataComponentMap;
//?} else {
/^import dev.doole.corecrucible.registry.ModArmorMaterials;
import net.minecraft.core.component.DataComponentPatch;
^///?}

// NeoForge's startup events: registers all of the mod's items, recipes etc., wires up NeoForge's events to the
// shared Events class, and hands off to the mod's common setup. The Fabric side does the same job in FabricEntrypoint.
@Mod(CoreCrucible.MOD_ID)
@EventBusSubscriber(modid = CoreCrucible.MOD_ID)
public class NeoforgeEntrypoint {

    // Runs before the registry events: the balance config changes values baked into items.
    public NeoforgeEntrypoint() {
        BalanceConfig.load(MultiLoaderUtil.INSTANCE.configDir());
    }

    // Registers every mod item, recipe serializer etc. into NeoForge's registries.
    @SubscribeEvent
    private static void onRegister(RegisterEvent event) {
        //? if <1.21.2 {
        /^event.register(Registries.ARMOR_MATERIAL, helper -> ModArmorMaterials.register(helper::register));
        ^///?}
        event.register(Registries.ITEM, helper -> ModItems.register(helper::register));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> ModCreativeTab.register(helper::register));
        event.register(Registries.RECIPE_SERIALIZER, helper -> ModRecipes.register(helper::register));
        //? if >=1.21.2 {
        event.register(Registries.RECIPE_BOOK_CATEGORY, helper -> ModRecipeBookCategories.register(helper::register));
        //?}
    }

    // Registers the two client-to-server network messages and hooks their handlers up to the shared Events class.
    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(VeinMining.ModePayload.TYPE, VeinMining.ModePayload.CODEC,
                        (payload, context) -> context.enqueueWork(() -> Events.veinMode((ServerPlayer) context.player(), payload)))
                .playToServer(SmithingRecipePlacer.PlacePayload.TYPE, SmithingRecipePlacer.PlacePayload.CODEC,
                        (payload, context) -> context.enqueueWork(() -> Events.placeSmithingRecipe((ServerPlayer) context.player(), payload)));
    }

    // Gives every vanilla item that joins the chain its tier's durability/repair defaults.
    @SubscribeEvent
    private static void onModifyComponents(ModifyDefaultComponentsEvent event) {
        VanillaOverrides.forEach((item, tier, armor) ->
                //? if >=1.21.2 {
                event.modify(item, (builder, context, it) -> VanillaOverrides.apply(tier, armor, sink(builder), context))
                //?} else {
                /^event.modify(item, builder -> VanillaOverrides.apply(tier, armor, sink(builder), null))
                ^///?}
        );
    }

    @SubscribeEvent
    // Runs once registries are done; the shared setup (dispensers, debug audit) goes on the main thread.
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CoreCrucible::onInitialize);
    }

    // Wraps NeoForge's default-component builder as the loader-neutral ComponentSink that VanillaOverrides expects.
    //? if >=1.21.2 {
    private static VanillaOverrides.ComponentSink sink(DataComponentMap.Builder builder) {
    //?} else {
    /^private static VanillaOverrides.ComponentSink sink(DataComponentPatch.Builder builder) {
    ^///?}
        return new VanillaOverrides.ComponentSink() {
            @Override
            public <T> void set(DataComponentType<T> type, T value) {
                builder.set(type, value);
            }
        };
    }
}
*///?}
