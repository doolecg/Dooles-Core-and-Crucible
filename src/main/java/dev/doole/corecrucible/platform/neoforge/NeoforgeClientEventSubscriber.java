package dev.doole.corecrucible.platform.neoforge;

//? neoforge {

/*import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.client.TieredShieldRenderer;
import dev.doole.corecrucible.client.TooltipHandler;
import dev.doole.corecrucible.client.VeinMiningClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
//? if >=1.21.2 {
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
//?} else {
/^import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
^///?}

// NeoForge's client-only startup events: the mod's client setup, item tooltips, and the vein-mining key.
@EventBusSubscriber(modid = CoreCrucible.MOD_ID, value = Dist.CLIENT)
public class NeoforgeClientEventSubscriber {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CoreCrucible::onInitializeClient);
    }

    @SubscribeEvent
    // Adds the mod's lines (tier, stats, next upgrade) to every item tooltip.
    public static void onTooltip(ItemTooltipEvent event) {
        TooltipHandler.append(event.getItemStack(), event.getToolTip());
    }

    @SubscribeEvent
    // The Vein Resonance key, so it shows up in Controls and can be rebound.
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(VeinMiningClient.VEIN_KEY);
    }

    // Tiered shields draw with vanilla's shield model and their own texture.
    //? if >=1.21.2 {
    @SubscribeEvent
    public static void onRegisterSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(TieredShieldRenderer.ID, TieredShieldRenderer.Unbaked.MAP_CODEC);
    }
    //?} else {
    /^@SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new BlockEntityWithoutLevelRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels()) {
                        @Override
                        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                                                 int light, int overlay) {
                            TieredShieldRenderer.render(stack, pose, buffers, light, overlay);
                        }
                    };
                }
                return renderer;
            }
        }, TieredShieldRenderer.shields().toArray(new Item[0]));
    }
    ^///?}
}
*///?}
