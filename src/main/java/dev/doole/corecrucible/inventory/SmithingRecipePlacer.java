package dev.doole.corecrucible.inventory;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.mixin.ItemCombinerMenuAccessor;
import dev.doole.corecrucible.recipe.SmithingRequirements;
import java.util.function.Predicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
//? if >=1.21.2 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//?}

/**
 * The smithing recipe book: moves a recipe's items from the player's inventory into the smithing table,
 * like vanilla's recipe book does for the crafting table. On 26.x the vanilla place-recipe packet arrives here through
 * {@code ServerGamePacketListenerImplMixin}; the 1.21.1 recipe book sends {@link PlacePayload}.
 */
public final class SmithingRecipePlacer {
    /** Main inventory and hotbar. */
    private static final int INVENTORY_SLOTS = 36;

    /** 1.21.1 only: place recipe {@code recipe}; {@code max} is vanilla's shift-click (as many crafts as fit). */
    public record PlacePayload(Identifier recipe, boolean max) implements CustomPacketPayload {
        public static final Type<PlacePayload> TYPE = new Type<>(CoreCrucible.id("place_smithing_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlacePayload> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, PlacePayload::recipe, ByteBufCodecs.BOOL, PlacePayload::max, PlacePayload::new);

        @Override
        public Type<PlacePayload> type() {
            return TYPE;
        }
    }

    private SmithingRecipePlacer() {
    }

    /**
     * Whether the inventory, plus what's already in the table, holds a full craft: template, base, addition and
     * catalyst. The table counts because placing returns its items to the inventory first.
     */
    public static boolean canCraft(Inventory inventory, SmithingMenu menu, SmithingRequirements req) {
        return crafts(inventory, menu, req) > 0;
    }

    /** How many whole crafts the inventory and table hold. */
    // The smallest count across the needed ingredients decides how many crafts are possible.
    private static int crafts(Inventory inventory, SmithingMenu menu, SmithingRequirements req) {
        int n = Integer.MAX_VALUE;
        if (req.template() != null) n = Math.min(n, count(inventory, menu, req.template()));
        n = Math.min(n, count(inventory, menu, req.base()));
        if (req.addition() != null) n = Math.min(n, count(inventory, menu, req.addition()));
        if (req.catalyst() != null) n = Math.min(n, count(inventory, menu, s -> s.is(req.catalyst())));
        return n;
    }

    // Counts matching items in the player's inventory, the table's three input slots and the catalyst slot.
    private static int count(Inventory inventory, SmithingMenu menu, Predicate<ItemStack> filter) {
        int total = 0;
        for (int i = 0; i < INVENTORY_SLOTS; i++) total += matching(inventory.getItem(i), filter);
        Container inputs = ((ItemCombinerMenuAccessor) menu).dooles_core_crucible$inputSlots();
        for (int i = 0; i < 3; i++) total += matching(inputs.getItem(i), filter);
        if (menu instanceof CatalystMenu catalyst) total += matching(catalyst.dooles_core_crucible$catalyst().getItem(0), filter);
        return total;
    }

    private static int matching(ItemStack stack, Predicate<ItemStack> filter) {
        return !stack.isEmpty() && filter.test(stack) ? stack.getCount() : 0;
    }

    /** 1.21.1: the payload's recipe, if the player knows it. Server thread. */
    public static void place(ServerPlayer player, PlacePayload payload) {
        if (!(player.containerMenu instanceof SmithingMenu) || player.isSpectator() || !player.containerMenu.stillValid(player)) return;
        //? if >=1.21.2 {
        RecipeHolder<?> recipe = player.level().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, payload.recipe())).orElse(null);
        if (recipe == null || !player.getRecipeBook().contains(recipe.id())) return;
        //?} else {
        /*RecipeHolder<?> recipe = player.server.getRecipeManager().byKey(payload.recipe()).orElse(null);
        if (recipe == null || !player.getRecipeBook().contains(recipe)) return;
        *///?}
        SmithingRequirements req = SmithingRequirements.of(recipe.value());
        if (req != null) place(player, req, payload.max());
    }

    /**
     * Puts the table's inputs back in the inventory, then moves one craft's items in ({@code max}: as many crafts as
     * fit). Returns false when the inventory lacks something, so the client shows the recipe as a ghost instead.
     */
    public static boolean place(ServerPlayer player, SmithingRequirements req, boolean max) {
        if (!(player.containerMenu instanceof SmithingMenu menu) || !(menu instanceof CatalystMenu catalystMenu)) return false;
        Inventory inventory = player.getInventory();
        Container inputs = ((ItemCombinerMenuAccessor) menu).dooles_core_crucible$inputSlots();
        Container catalyst = catalystMenu.dooles_core_crucible$catalyst();

        // Clear the table first, like vanilla's recipe book.
        for (int i = 0; i < 3; i++) giveBack(player, inputs.removeItemNoUpdate(i));
        giveBack(player, catalyst.removeItemNoUpdate(0));

        int n = crafts(inventory, menu, req);
        boolean placed = n > 0;
        // One craft normally; shift-click (max) moves as many crafts as the inventory holds.
        if (placed) {
            if (!max) n = 1;
            if (req.template() != null) take(inventory, req.template(), n, inputs, SmithingMenu.TEMPLATE_SLOT);
            take(inventory, req.base(), n, inputs, SmithingMenu.BASE_SLOT);
            if (req.addition() != null) take(inventory, req.addition(), n, inputs, SmithingMenu.ADDITIONAL_SLOT);
            if (req.catalyst() != null) take(inventory, s -> s.is(req.catalyst()), n, catalyst, 0);
        }
        // Recalculate the result slot and send the new slot contents to the client.
        menu.slotsChanged(inputs);
        menu.broadcastChanges();
        return placed;
    }

    private static void giveBack(ServerPlayer player, ItemStack stack) {
        // add() fills what fits; spawn the rest at the player's feet (drop() differs on every version).
        if (stack.isEmpty() || player.getInventory().add(stack) || stack.isEmpty()) return;
        player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
    }

    /** Moves up to {@code amount} matching items (all the same kind as the first found, one stack) into {@code slot}. */
    private static void take(Inventory inventory, Predicate<ItemStack> filter, int amount, Container target, int slot) {
        ItemStack placed = ItemStack.EMPTY;
        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            int limit = placed.isEmpty() ? amount : Math.min(amount, target.getMaxStackSize(placed));
            if (placed.getCount() >= limit) break;
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !filter.test(stack)) continue;
            if (!placed.isEmpty() && !ItemStack.isSameItemSameComponents(placed, stack)) continue;
            int room = Math.min(amount, target.getMaxStackSize(stack)) - placed.getCount();
            ItemStack split = stack.split(Math.min(room, stack.getCount()));
            if (placed.isEmpty()) placed = split;
            else placed.grow(split.getCount());
        }
        if (!placed.isEmpty()) target.setItem(slot, placed);
        inventory.setChanged();
    }
}
