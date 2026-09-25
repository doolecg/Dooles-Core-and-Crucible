package dev.doole.corecrucible.client;

//? if >=1.21.2 {
import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.inventory.CatalystMenu;
import dev.doole.corecrucible.inventory.SmithingRecipePlacer;
import dev.doole.corecrucible.mixin.client.GhostSlotsInvoker;
import dev.doole.corecrucible.mixin.client.RecipeCollectionAccessor;
import dev.doole.corecrucible.recipe.SmithingRequirements;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModRecipeBookCategories;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;

/**
 * The smithing table's recipe book on 26.x: vanilla's {@link RecipeBookComponent}, with the smithing
 * table's slots, the mod's recipe-book categories as tabs (plus vanilla's own smithing category) and the catalyst
 * slot. Everything else (search box, craftable filter, pages, tab animations, ghost recipes, clicks and keys) is
 * vanilla's code.
 *
 * <p>Vanilla's component needs a {@link RecipeBookMenu}, which the smithing menu isn't, so {@link BookMenu} stands in
 * for it; the component only asks it for the table's stacked contents and a book type. The book's open and filter
 * state is kept apart from that type's ({@code RecipeBookComponentMixin}).
 */
public class SmithingRecipeBookComponent extends RecipeBookComponent<SmithingRecipeBookComponent.BookMenu> {
    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted"));
    private static final Component ONLY_SMITHABLE = Component.translatable("gui." + CoreCrucible.MOD_ID + ".recipebook.toggleRecipes.smithable");

    // Remembered while the game runs; vanilla keeps a book type's state in the player's saved settings, which have
    // no room for a fifth book.
    static boolean open;
    static boolean filtering;

    private final SmithingMenu smithing;

    public SmithingRecipeBookComponent(SmithingMenu smithing) {
        super(new BookMenu(smithing), tabs());
        this.smithing = smithing;
    }

    // The book's tabs, top to bottom: All (compass icon), Gear, Alloys, then vanilla's own smithing recipes (trims and
    // the netherite upgrade).
    private static List<TabInfo> tabs() {
        return List.of(
                new TabInfo(new ItemStack(Items.COMPASS), Optional.empty(), ModRecipeBookCategories.ALL),
                new TabInfo(Items.IRON_PICKAXE, Items.IRON_CHESTPLATE, ModRecipeBookCategories.GEAR),
                new TabInfo(ModItems.alloyIngot(ModTiers.IRON), ModRecipeBookCategories.ALLOYS),
                new TabInfo(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, RecipeBookCategories.SMITHING));
    }

    /** Whether {@code component} is a smithing book (the mixins on vanilla's component check this). */
    public static boolean is(Object component) {
        return component instanceof SmithingRecipeBookComponent;
    }

    public static boolean isOpen() {
        return open;
    }

    public static void setOpen(boolean value) {
        open = value;
    }

    public static boolean isFiltering() {
        return filtering;
    }

    public static void setFiltering(boolean value) {
        filtering = value;
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_SPRITES;
    }

    @Override
    // The slots the book fills: the table's three inputs and the catalyst slot.
    protected boolean isCraftingSlot(Slot slot) {
        return slot.container == smithing.getSlot(0).container || slot == catalystSlot();
    }

    private Slot catalystSlot() {
        return smithing instanceof CatalystMenu catalyst ? catalyst.dooles_core_crucible$catalystSlot() : null;
    }

    /**
     * Vanilla's craftable check doesn't know about the catalyst slot, so recipes whose catalyst the inventory can't
     * cover are taken back out of the craftable set.
     */
    @Override
    protected void selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents) {
        collection.selectRecipes(stackedContents, display -> display instanceof SmithingRecipeDisplay);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        Inventory inventory = minecraft.player.getInventory();
        ContextMap context = SlotDisplayContext.fromLevel(minecraft.level);
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            if (!collection.isCraftable(entry.id()) || !(entry.display() instanceof SmithingRecipeDisplay display)) continue;
            if (!SmithingRecipePlacer.canCraft(inventory, smithing, SmithingRequirements.of(display, context))) {
                ((RecipeCollectionAccessor) collection).dooles_core_crucible$craftable().remove(entry.id());
            }
        }
    }

    @Override
    protected Component getRecipeFilterName() {
        return ONLY_SMITHABLE;
    }

    @Override
    // Show a recipe's items faded in the table's slots when the player can't craft it yet.
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipe, ContextMap context) {
        if (!(recipe instanceof SmithingRecipeDisplay display)) return;
        GhostSlotsInvoker ghost = (GhostSlotsInvoker) ghostSlots;
        ghost.dooles_core_crucible$setResult(smithing.getSlot(SmithingMenu.RESULT_SLOT), context, display.result());
        ghost.dooles_core_crucible$setInput(smithing.getSlot(SmithingMenu.TEMPLATE_SLOT), context, display.template());
        ghost.dooles_core_crucible$setInput(smithing.getSlot(SmithingMenu.BASE_SLOT), context, display.base());
        ghost.dooles_core_crucible$setInput(smithing.getSlot(SmithingMenu.ADDITIONAL_SLOT), context, display.addition());
    }

    /** Stands in for the {@link RecipeBookMenu} vanilla's component expects; placing goes through the server mixin. */
    public static final class BookMenu extends RecipeBookMenu {
        private final SmithingMenu smithing;

        BookMenu(SmithingMenu smithing) {
            super(null, smithing.containerId);
            this.smithing = smithing;
        }

        @Override
        // Never called on the client; the server does the placing (ServerGamePacketListenerImplMixin).
        public PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear, RecipeHolder<?> recipe, ServerLevel level, Inventory inventory) {
            return PostPlaceAction.NOTHING;
        }

        /** The table's inputs count as available, as the crafting grid's do in vanilla. */
        @Override
        public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
            for (int i = 0; i < 3; i++) stackedContents.accountStack(smithing.getSlot(i).getItem());
        }

        @Override
        // Vanilla needs some book type here; the open/filter state is kept separately (see RecipeBookComponentMixin).
        public RecipeBookType getRecipeBookType() {
            return RecipeBookType.CRAFTING;
        }

        @Override
        public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }
    }
}
//?}
