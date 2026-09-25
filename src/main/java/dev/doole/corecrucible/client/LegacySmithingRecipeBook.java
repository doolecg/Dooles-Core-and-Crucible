package dev.doole.corecrucible.client;

//? if <1.21.2 {
/*import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.inventory.SmithingRecipePlacer;
import dev.doole.corecrucible.recipe.ModSmithingRecipe;
import dev.doole.corecrucible.recipe.SmithingRequirements;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;

/^*
 * The smithing table's recipe book on 1.21.1. Vanilla's 1.21.1 widget can't be reused here: it makes
 * itself the player's open menu and reads recipe ingredients that smithing recipes don't list. So this is a copy of
 * it, drawn from vanilla's own textures and sprites with vanilla's layout: tabs on the left, search box, craftable
 * filter, a 5×4 grid of green/red recipe slots, page arrows, and a ghost recipe in the table when you lack items.
 * Only recipes the player has unlocked are shown, as in vanilla.
 ^/
public class LegacySmithingRecipeBook implements GuiEventListener, NarratableEntry {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted"));
    private static final WidgetSprites PAGE_FORWARD = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_forward"), Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted"));
    private static final WidgetSprites PAGE_BACKWARD = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_backward"), Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted"));
    private static final WidgetSprites TAB = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/tab"), Identifier.withDefaultNamespace("recipe_book/tab_selected"));
    private static final Identifier SLOT_CRAFTABLE = Identifier.withDefaultNamespace("recipe_book/slot_craftable");
    private static final Identifier SLOT_UNCRAFTABLE = Identifier.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);
    private static final Component ONLY_SMITHABLE = Component.translatable("gui." + CoreCrucible.MOD_ID + ".recipebook.toggleRecipes.smithable");
    private static final Component ALL_RECIPES = Component.translatable("gui.recipebook.toggleRecipes.all");
    // 20 recipes per page: a grid of 5 across and 4 down.
    private static final int PER_PAGE = 20;

    private enum Tab { ALL, GEAR, ALLOYS, SMITHING }

    // Remembered while the game runs, like the 26.x book.
    private static boolean open;
    private static boolean filtering;
    private static Tab tab = Tab.ALL;

    // Screen layout (set in init), the book's widgets, which page and recipes are showing, the recipe under the mouse,
    // the last recipe clicked and its ghost items. time drives the cycling icons for recipes with several options.
    private final SmithingMenu menu;
    private Minecraft minecraft;
    private int width;
    private int height;
    private boolean widthTooNarrow;
    private int xOffset;
    private EditBox searchBox;
    private StateSwitchingButton filterButton;
    private StateSwitchingButton forwardButton;
    private StateSwitchingButton backButton;
    private int page;
    private List<RecipeHolder<?>> shown = List.of();
    private final List<Tab> tabs = new ArrayList<>();
    private RecipeHolder<?> hovered;
    private RecipeHolder<?> lastPlaced;
    private List<List<ItemStack>> ghost;
    private float time;
    private boolean ignoreTextInput;

    public LegacySmithingRecipeBook(SmithingMenu menu) {
        this.menu = menu;
    }

    public void init(int width, int height, Minecraft minecraft, boolean widthTooNarrow) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        this.widthTooNarrow = widthTooNarrow;
        if (open) initVisuals();
    }

    private void initVisuals() {
        xOffset = widthTooNarrow ? 0 : 86;
        int x = xOrigin();
        int y = yOrigin();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(minecraft.font, x + 25, y + 13, 81, 9 + 5, Component.translatable("itemGroup.search"));
        searchBox.setMaxLength(50);
        searchBox.setVisible(true);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(old);
        searchBox.setHint(SEARCH_HINT);
        filterButton = new StateSwitchingButton(x + 110, y + 12, 26, 16, filtering);
        filterButton.initTextureValues(FILTER_SPRITES);
        updateFilterTooltip();
        forwardButton = new StateSwitchingButton(x + 93, y + 137, 12, 17, false);
        forwardButton.initTextureValues(PAGE_FORWARD);
        backButton = new StateSwitchingButton(x + 38, y + 137, 12, 17, true);
        backButton.initTextureValues(PAGE_BACKWARD);
        refresh(false);
    }

    private int xOrigin() {
        return (width - 147) / 2 - xOffset;
    }

    private int yOrigin() {
        return (height - 166) / 2;
    }

    public int updateScreenPosition(int width, int imageWidth) {
        return isVisible() && !widthTooNarrow ? 177 + (width - imageWidth - 200) / 2 : (width - imageWidth) / 2;
    }

    public boolean isVisible() {
        return open;
    }

    public void toggleVisibility() {
        setVisible(!open);
    }

    private void setVisible(boolean visible) {
        if (visible) initVisuals();
        open = visible;
    }

    private boolean isOffsetNextToMainGui() {
        return xOffset == 86;
    }

    private void updateFilterTooltip() {
        filterButton.setTooltip(Tooltip.create(filtering ? ONLY_SMITHABLE : ALL_RECIPES));
    }

    // ---- Recipes --------------------------------------------------------------------------------------------

    private static Tab tabOf(RecipeHolder<?> holder) {
        if (!(holder.value() instanceof ModSmithingRecipe mod)) return Tab.SMITHING;
        return switch (mod.bookTab()) {
            case GEAR -> Tab.GEAR;
            case ALLOYS -> Tab.ALLOYS;
        };
    }

    /^* Unlocked smithing recipes, grouped by tab. ^/
    private List<RecipeHolder<?>> known() {
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (RecipeHolder<SmithingRecipe> holder : minecraft.level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING)) {
            if (minecraft.player.getRecipeBook().contains(holder)) out.add(holder);
        }
        out.sort(Comparator.<RecipeHolder<?>>comparingInt(h -> tabOf(h).ordinal()).thenComparing(h -> h.id().toString()));
        return out;
    }

    private ItemStack result(RecipeHolder<?> holder) {
        return holder.value() instanceof ModSmithingRecipe mod ? mod.resultPreview() : holder.value().getResultItem(minecraft.level.registryAccess());
    }

    private boolean craftable(RecipeHolder<?> holder) {
        SmithingRequirements req = SmithingRequirements.of(holder.value());
        return req != null && SmithingRecipePlacer.canCraft(minecraft.player.getInventory(), menu, req);
    }

    // Rebuilds the visible list: pick the tabs that have recipes, then keep recipes matching the current tab, the search
    // text and (if on) the "only smithable" filter.
    private void refresh(boolean resetPage) {
        List<RecipeHolder<?>> known = known();
        tabs.clear();
        tabs.add(Tab.ALL);
        for (Tab t : Tab.values()) {
            if (t != Tab.ALL && known.stream().anyMatch(h -> tabOf(h) == t)) tabs.add(t);
        }
        if (!tabs.contains(tab)) tab = Tab.ALL;
        String search = searchBox.getValue().toLowerCase(Locale.ROOT);
        List<RecipeHolder<?>> list = new ArrayList<>();
        for (RecipeHolder<?> holder : known) {
            if (tab != Tab.ALL && tabOf(holder) != tab) continue;
            if (!search.isEmpty() && !result(holder).getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)) continue;
            if (filtering && !craftable(holder)) continue;
            list.add(holder);
        }
        shown = list;
        int pages = pages();
        if (resetPage || page >= pages) page = 0;
        forwardButton.visible = pages > 1 && page < pages - 1;
        backButton.visible = pages > 1 && page > 0;
    }

    private int pages() {
        return (int) Math.ceil(shown.size() / (double) PER_PAGE);
    }

    // Icons drawn on each tab.
    private static List<ItemStack> icons(Tab t) {
        return switch (t) {
            case ALL -> List.of(new ItemStack(Items.COMPASS));
            case GEAR -> List.of(new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_CHESTPLATE));
            case ALLOYS -> List.of(new ItemStack(ModItems.alloyIngot(ModTiers.IRON)));
            case SMITHING -> List.of(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE));
        };
    }

    // ---- Rendering ------------------------------------------------------------------------------------------

    // Draws the whole book: background, search box, tabs, the recipe grid (green = can craft, red = can't), page buttons.
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hovered = null;
        if (!open) return;
        if (!Screen.hasControlDown()) time += partialTick;
        refresh(false);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        int x = xOrigin();
        int y = yOrigin();
        graphics.blit(TEXTURE, x, y, 1, 1, 147, 166);
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            boolean selected = t == tab;
            int tx = x - 30 - (selected ? 2 : 0);
            int ty = y + 3 + 27 * i;
            graphics.blitSprite(TAB.get(true, selected), tx, ty, 35, 27);
            List<ItemStack> icons = icons(t);
            if (icons.size() == 1) {
                graphics.renderFakeItem(icons.get(0), tx + 9, ty + 5);
            } else {
                graphics.renderFakeItem(icons.get(0), tx + 3, ty + 5);
                graphics.renderFakeItem(icons.get(1), tx + 14, ty + 5);
            }
        }
        filterButton.render(graphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < PER_PAGE; i++) {
            int n = page * PER_PAGE + i;
            if (n >= shown.size()) break;
            RecipeHolder<?> holder = shown.get(n);
            int bx = x + 11 + 25 * (i % 5);
            int by = y + 31 + 25 * (i / 5);
            graphics.blitSprite(craftable(holder) ? SLOT_CRAFTABLE : SLOT_UNCRAFTABLE, bx, by, 25, 25);
            graphics.renderFakeItem(result(holder), bx + 4, by + 4);
            if (mouseX >= bx && mouseY >= by && mouseX < bx + 25 && mouseY < by + 25) hovered = holder;
        }
        int pages = pages();
        if (pages > 1) {
            Component label = Component.translatable("gui.recipebook.page", page + 1, pages);
            graphics.drawString(minecraft.font, label, x - minecraft.font.width(label) / 2 + 73, y + 141, -1, false);
        }
        backButton.render(graphics, mouseX, mouseY, partialTick);
        forwardButton.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (open && hovered != null) {
            graphics.renderComponentTooltip(minecraft.font, Screen.getTooltipFromItem(minecraft, result(hovered)), mouseX, mouseY);
        }
    }

    /^* The ghost recipe over the table's template, base, addition and result slots, as vanilla draws it. ^/
    public void renderGhostRecipe(GuiGraphics graphics, int leftPos, int topPos) {
        if (ghost == null) return;
        int index = (int) (time / 30.0F);
        for (int slot = 0; slot < ghost.size(); slot++) {
            List<ItemStack> stacks = ghost.get(slot);
            if (stacks.isEmpty()) continue;
            Slot target = menu.getSlot(slot);
            int x = leftPos + target.x;
            int y = topPos + target.y;
            ItemStack stack = stacks.get(index % stacks.size());
            graphics.fill(x, y, x + 16, y + 16, 0x30FF0000);
            graphics.renderFakeItem(stack, x, y);
            graphics.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, 0x30FFFFFF);
            if (slot == SmithingMenu.RESULT_SLOT) graphics.renderItemDecorations(minecraft.font, stack, x, y);
        }
    }

    // ---- Input ----------------------------------------------------------------------------------------------

    @Override
    // Checks, in order: a recipe in the grid, the search box, the filter button, the page arrows, then the tabs.
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || minecraft.player.isSpectator()) return false;
        int x = xOrigin();
        int y = yOrigin();
        if (button == 0) {
            for (int i = 0; i < PER_PAGE; i++) {
                int n = page * PER_PAGE + i;
                if (n >= shown.size()) break;
                int bx = x + 11 + 25 * (i % 5);
                int by = y + 31 + 25 * (i / 5);
                if (mouseX >= bx && mouseY >= by && mouseX < bx + 25 && mouseY < by + 25) {
                    click();
                    place(shown.get(n), Screen.hasShiftDown());
                    if (!isOffsetNextToMainGui()) setVisible(false);
                    return true;
                }
            }
        }
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            searchBox.setFocused(true);
            return true;
        }
        searchBox.setFocused(false);
        if (filterButton.mouseClicked(mouseX, mouseY, button)) {
            filtering = !filtering;
            filterButton.setStateTriggered(filtering);
            updateFilterTooltip();
            refresh(false);
            return true;
        }
        if (forwardButton.mouseClicked(mouseX, mouseY, button)) {
            page++;
            refresh(false);
            return true;
        }
        if (backButton.mouseClicked(mouseX, mouseY, button)) {
            page--;
            refresh(false);
            return true;
        }
        for (int i = 0; i < tabs.size(); i++) {
            int ty = y + 3 + 27 * i;
            if (button == 0 && mouseX >= x - 30 && mouseX < x + 5 && mouseY >= ty && mouseY < ty + 27) {
                click();
                if (tab != tabs.get(i)) {
                    tab = tabs.get(i);
                    refresh(true);
                }
                return true;
            }
        }
        return false;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /^* Like vanilla: fill the table if the inventory holds everything, otherwise show the recipe as a ghost. ^/
    private void place(RecipeHolder<?> holder, boolean max) {
        boolean craftable = craftable(holder);
        if (!craftable && holder.equals(lastPlaced)) return;
        lastPlaced = holder;
        ghost = craftable ? null : ghostStacks(holder);
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new SmithingRecipePlacer.PlacePayload(holder.id(), max)));
        }
    }

    private List<List<ItemStack>> ghostStacks(RecipeHolder<?> holder) {
        if (holder.value() instanceof ModSmithingRecipe mod) return mod.ghostStacks();
        return List.of(List.of(), List.of(), List.of(), List.of(result(holder)));
    }

    // Touching one of the table's own slots clears the ghost recipe.
    public void slotClicked(Slot slot) {
        if (slot != null && slot.index <= SmithingMenu.RESULT_SLOT) {
            ghost = null;
            lastPlaced = null;
        }
    }

    public boolean hasClickedOutside(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth, int imageHeight) {
        if (!open) return true;
        boolean outside = mouseX < leftPos || mouseY < topPos || mouseX >= leftPos + imageWidth || mouseY >= topPos + imageHeight;
        boolean onBook = leftPos - 147 < mouseX && mouseX < leftPos && topPos < mouseY && mouseY < topPos + imageHeight;
        return outside && !onBook;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ignoreTextInput = false;
        if (!open || minecraft.player.isSpectator()) return false;
        if (keyCode == 256 && !isOffsetNextToMainGui()) {
            setVisible(false);
            return true;
        }
        if (searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (searchBox.isFocused() && searchBox.isVisible() && keyCode != 256) return true;
        if (minecraft.options.keyChat.matches(keyCode, scanCode) && !searchBox.isFocused()) {
            ignoreTextInput = true;
            searchBox.setFocused(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        ignoreTextInput = false;
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (ignoreTextInput || !open || minecraft.player.isSpectator()) return false;
        return searchBox.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return open ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
    }
}
*///?}
