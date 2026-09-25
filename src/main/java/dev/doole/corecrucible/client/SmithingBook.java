package dev.doole.corecrucible.client;

import dev.doole.corecrucible.mixin.client.AbstractContainerScreenAccessor;
import dev.doole.corecrucible.mixin.client.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.Slot;
//? if >=1.21.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * The smithing table's recipe book, wired into the smithing screen the way vanilla's
 * {@code AbstractRecipeBookScreen} wires the crafting table's: the green book button, the table shifting right, and
 * the book getting clicks, keys and drawing first. The book itself is vanilla's widget on 26.x
 * ({@link SmithingRecipeBookComponent}) and a copy of it on 1.21.1 ({@link LegacySmithingRecipeBook}).
 * The hooks live in {@code ItemCombinerScreenMixin}, {@code AbstractContainerScreenMixin} and {@code SmithingScreenMixin}.
 */
public final class SmithingBook {
    // The one book and the smithing screen it belongs to. Only one smithing screen can be open at a time.
    // widthTooNarrow: the window is too narrow to show the book beside the table (under 379 pixels wide).
    //? if >=1.21.2 {
    private static SmithingRecipeBookComponent book;
    //?} else {
    /*private static LegacySmithingRecipeBook book;
    *///?}
    private static SmithingScreen screen;
    private static boolean widthTooNarrow;

    private SmithingBook() {
    }

    /** After the smithing screen's {@code init} (also on resize, which keeps the book and its search text). */
    public static void init(SmithingScreen smithing) {
        if (screen != smithing || book == null) {
            screen = smithing;
            //? if >=1.21.2 {
            book = new SmithingRecipeBookComponent(smithing.getMenu());
            //?} else {
            /*book = new LegacySmithingRecipeBook(smithing.getMenu());
            *///?}
        }
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) smithing;
        widthTooNarrow = smithing.width < 379;
        book.init(smithing.width, smithing.height, Minecraft.getInstance(), widthTooNarrow);
        // Shift the table right when the book is open, and add the green recipe book button that opens/closes it.
        acc.dooles_core_crucible$setLeftPos(book.updateScreenPosition(smithing.width, acc.dooles_core_crucible$imageWidth()));
        ImageButton button = new ImageButton(buttonX(acc), buttonY(acc), 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, b -> {
            book.toggleVisibility();
            acc.dooles_core_crucible$setLeftPos(book.updateScreenPosition(smithing.width, acc.dooles_core_crucible$imageWidth()));
            b.setPosition(buttonX(acc), buttonY(acc));
        });
        ((ScreenAccessor) smithing).dooles_core_crucible$addRenderableWidget(button);
    }

    // Right under the result slot: the hammer fills the top-left and the armor stand the right.
    private static int buttonX(AbstractContainerScreenAccessor acc) {
        return acc.dooles_core_crucible$leftPos() + 96;
    }

    private static int buttonY(AbstractContainerScreenAccessor acc) {
        return acc.dooles_core_crucible$topPos() + 65;
    }

    // True only for the smithing screen that owns the book. Every method below checks this first, so the mixin hooks do
    // nothing on other screens.
    private static boolean active(Object candidate) {
        return candidate == screen && book != null;
    }

    /** A narrow window shows the open book instead of the table, as vanilla does. */
    public static boolean hidesContents(Object candidate) {
        return active(candidate) && widthTooNarrow && book.isVisible();
    }

    private static void focus(Object candidate) {
        ((Screen) candidate).setFocused(book);
    }

    public static void slotClicked(Object candidate, Slot slot) {
        if (active(candidate)) book.slotClicked(slot);
    }

    /** Vanilla's rule: a click outside both the table and the open book drops the carried item. */
    public static Boolean hasClickedOutside(Object candidate, double mouseX, double mouseY) {
        if (!active(candidate)) return null;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) candidate;
        int left = acc.dooles_core_crucible$leftPos();
        int top = acc.dooles_core_crucible$topPos();
        int w = acc.dooles_core_crucible$imageWidth();
        int h = acc.dooles_core_crucible$imageHeight();
        boolean outside = mouseX < left || mouseY < top || mouseX >= left + w || mouseY >= top + h;
        return book.hasClickedOutside(mouseX, mouseY, left, top, w, h) && outside;
    }

    //? if >=1.21.2 {
    public static void render(Object candidate, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!active(candidate)) return;
        graphics.nextStratum();
        book.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    public static void renderTooltip(Object candidate, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (active(candidate)) book.extractTooltip(graphics, mouseX, mouseY, ((AbstractContainerScreenAccessor) candidate).dooles_core_crucible$hoveredSlot());
    }

    /** Inside the slot pass (translated to the table's corner), like vanilla. */
    public static void renderGhost(Object candidate, GuiGraphicsExtractor graphics) {
        if (active(candidate)) book.extractGhostRecipe(graphics, false);
    }

    // The book gets the click first. On a narrow window the open book covers the table, so the table gets no clicks.
    public static boolean mouseClicked(Object candidate, MouseButtonEvent event, boolean doubleClick) {
        if (!active(candidate)) return false;
        if (book.mouseClicked(event, doubleClick)) {
            focus(candidate);
            return true;
        }
        return widthTooNarrow && book.isVisible();
    }

    public static boolean mouseDragged(Object candidate, MouseButtonEvent event, double dx, double dy) {
        return active(candidate) && book.mouseDragged(event, dx, dy);
    }

    public static boolean keyPressed(Object candidate, KeyEvent event) {
        if (!active(candidate) || !book.keyPressed(event)) return false;
        focus(candidate);
        return true;
    }

    public static void tick(Object candidate) {
        if (active(candidate)) book.tick();
    }

    public static void recipesUpdated(Object candidate) {
        if (active(candidate)) book.recipesUpdated();
    }

    public static void fillGhostRecipe(Object candidate, RecipeDisplay display) {
        if (active(candidate)) book.fillGhostRecipe(display);
    }
    //?} else {
    /*public static void render(Object candidate, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active(candidate)) return;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) candidate;
        book.render(graphics, mouseX, mouseY, partialTick);
        if (!hidesContents(candidate)) book.renderGhostRecipe(graphics, acc.dooles_core_crucible$leftPos(), acc.dooles_core_crucible$topPos());
    }

    public static void renderTooltip(Object candidate, GuiGraphics graphics, int mouseX, int mouseY) {
        if (active(candidate)) book.renderTooltip(graphics, mouseX, mouseY);
    }

    public static boolean mouseClicked(Object candidate, double mouseX, double mouseY, int button) {
        if (!active(candidate)) return false;
        if (book.mouseClicked(mouseX, mouseY, button)) {
            focus(candidate);
            return true;
        }
        return widthTooNarrow && book.isVisible();
    }

    public static boolean keyPressed(Object candidate, int keyCode, int scanCode, int modifiers) {
        if (!active(candidate) || !book.keyPressed(keyCode, scanCode, modifiers)) return false;
        focus(candidate);
        return true;
    }
    *///?}
}
