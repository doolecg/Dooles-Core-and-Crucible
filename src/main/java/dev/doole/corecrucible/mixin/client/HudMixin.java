package dev.doole.corecrucible.mixin.client;

import dev.doole.corecrucible.client.VeinMiningClient;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
//?} else {
/*import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
*///?}

// Draws the Vein Resonance shape menu in the top-left corner while its key is held.
//? if >=1.21.2 {
@Mixin(Hud.class)
//?} else {
/*@Mixin(Gui.class)
*///?}
public abstract class HudMixin {
    //? if >=1.21.2 {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dooles_core_crucible$veinMenu(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "render", at = @At("TAIL"))
    private void dooles_core_crucible$veinMenu(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    *///?}
        // A title with the block count, then one line per shape (locked ones say which level unlocks them). Empty when the
        // key isn't held or the tool has no Vein Resonance.
        List<Component> lines = VeinMiningClient.menuLines();
        if (lines.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        int width = 0;
        for (Component line : lines) width = Math.max(width, font.width(line));
        int x = 6;
        int y = 6;
        int lineHeight = font.lineHeight + 2;
        // Semi-transparent black box behind the text (0x90 = alpha).
        graphics.fill(x - 3, y - 3, x + width + 3, y + lines.size() * lineHeight + 1, 0x90000000);
        for (Component line : lines) {
            //? if >=1.21.2 {
            graphics.text(font, line, x, y, 0xFFFFFFFF);
            //?} else {
            /*graphics.drawString(font, line, x, y, 0xFFFFFFFF);
            *///?}
            y += lineHeight;
        }
    }
}
