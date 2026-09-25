package dev.doole.corecrucible.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Client APIs that differ between 1.21.1 and 26.x. */
public final class ClientCompat {

    private ClientCompat() {
    }

    // Whether Shift is held (the method moved between versions).
    public static boolean shiftDown() {
        //? if >=1.21.2 {
        return Minecraft.getInstance().hasShiftDown();
        //?} else {
        /*return Screen.hasShiftDown();
        *///?}
    }

    /** The open screen, or null in game. */
    public static Screen screen(Minecraft minecraft) {
        //? if >=1.21.2 {
        return minecraft.gui.screen();
        //?} else {
        /*return minecraft.screen;
        *///?}
    }
}
