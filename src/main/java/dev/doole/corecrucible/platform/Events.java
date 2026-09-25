package dev.doole.corecrucible.platform;

import dev.doole.corecrucible.enchant.EnchantmentHooks;
import dev.doole.corecrucible.enchant.VeinMining;
import dev.doole.corecrucible.gameplay.ShieldGuard;
import dev.doole.corecrucible.inventory.SmithingRecipePlacer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * A middleman between game events and the mod's gameplay code. Fabric events, NeoForge events and the mixins in
 * {@code mixin/} all end up calling one of the methods below, so the actual gameplay code never has to know which
 * loader fired the event or which mixin it came from.
 *
 * <ul>
 *   <li>{@link #serverTick}, {@link #playerTick}, {@link #livingHurt}: called from mixins shared by both loaders</li>
 *   <li>{@link #livingDamaged}: Fabric's {@code AFTER_DAMAGE} event; NeoForge's {@code LivingDamageEvent.Post} and
 *       {@code LivingShieldBlockEvent}</li>
 *   <li>{@link #serverStopping}: Fabric's {@code SERVER_STOPPING} event; NeoForge's {@code ServerStoppingEvent}</li>
 *   <li>{@link #veinMode}, {@link #placeSmithingRecipe}: handle the client-to-server network messages</li>
 * </ul>
 */
public final class Events {

    private Events() {
    }

    public static void serverTick(MinecraftServer server) {
        EnchantmentHooks.onServerTick(server);
    }

    public static void playerTick(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        EnchantmentHooks.onPlayerTick(level, player);
        ShieldGuard.tick(player);
    }

    /** Before a living entity takes damage; returns true to cancel it. */
    public static boolean livingHurt(LivingEntity victim, DamageSource source) {
        return victim.level() instanceof ServerLevel level && EnchantmentHooks.onLivingHurt(level, victim, source);
    }

    /** Before a living entity takes damage: the damage it should take instead (a raised shield's side cover). */
    public static float livingHurtAmount(LivingEntity victim, DamageSource source, float amount) {
        return victim.level() instanceof ServerLevel ? ShieldGuard.sideCover(victim, source, amount) : amount;
    }

    /** After a living entity took {@code damage}, or after a shield blocked the hit ({@code blocked}). */
    public static void livingDamaged(LivingEntity victim, DamageSource source, float damage, boolean blocked) {
        if (!(victim.level() instanceof ServerLevel level)) return;
        EnchantmentHooks.onLivingDamaged(level, victim, source, blocked);
    }

    public static void serverStopping(MinecraftServer server) {
        EnchantmentHooks.onServerStopping(server);
    }

    public static void veinMode(ServerPlayer player, VeinMining.ModePayload payload) {
        VeinMining.onModePayload(player, payload);
    }

    public static void placeSmithingRecipe(ServerPlayer player, SmithingRecipePlacer.PlacePayload payload) {
        SmithingRecipePlacer.place(player, payload);
    }
}
