package dev.doole.corecrucible.platform.fabric;

//? fabric {

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.client.TieredShieldRenderer;
import dev.doole.corecrucible.client.TooltipHandler;
import dev.doole.corecrucible.client.VeinMiningClient;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
//? if >=1.21.2 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.world.item.Item;
*///?}

// Fabric's client-side startup hook: sets up the things that only exist on the client (the vein-mining key,
// item tooltips), then runs the mod's shared client setup.
@Entrypoint("client")
public class FabricClientEntrypoint implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //? if >=1.21.2 {
        KeyMappingHelper.registerKeyMapping(VeinMiningClient.VEIN_KEY);
        //?} else {
        /*KeyBindingHelper.registerKeyBinding(VeinMiningClient.VEIN_KEY);
        *///?}
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> TooltipHandler.append(stack, lines));
        // Tiered shields draw with vanilla's shield model and their own texture.
        //? if >=1.21.2 {
        SpecialModelRenderers.ID_MAPPER.put(TieredShieldRenderer.ID, TieredShieldRenderer.Unbaked.MAP_CODEC);
        //?} else {
        /*for (Item shield : TieredShieldRenderer.shields()) {
            BuiltinItemRendererRegistry.INSTANCE.register(shield, (stack, mode, pose, buffers, light, overlay) ->
                    TieredShieldRenderer.render(stack, pose, buffers, light, overlay));
        }
        *///?}
        CoreCrucible.onInitializeClient();
    }
}
//?}
