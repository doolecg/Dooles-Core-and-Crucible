package dev.doole.corecrucible.enchant;

import com.mojang.math.Transformation;
import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.mixin.AbstractArrowAccessor;
import dev.doole.corecrucible.mixin.BlockDisplayAccessor;
import dev.doole.corecrucible.mixin.DisplayAccessor;
import dev.doole.corecrucible.registry.ModEnchantments;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
//? if >=1.21.2 {
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//?} else {
/*import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
*///?}

/**
 * Behaviour of the 15 enchantments ported from Applied-Enchantments. Soul Syphon's XP and durability
 * parts and Gravitic Anchor's explosion immunity are vanilla effect components in the enchantment JSON; everything
 * else is here. Entry points are called from {@link dev.doole.corecrucible.platform.Events} and from mixins.
 */
public final class EnchantmentHooks {
    // Tuning numbers. DARK: light level below which it counts as dark. ECHO_TICKS: how long Cavernous Echo glows and
    // its cooldown (20 ticks = 1 second). TETHER_TICKS: Soul Tether's length. VOLATILE_BASE_POWER: explosion size.
    private static final int DARK = 7;
    private static final BlockState ETHEREAL_LIGHT = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12);
    private static final int ECHO_TICKS = 120;
    private static final int ECHO_MAX_ORES = 64;
    private static final int TETHER_TICKS = 200;
    private static final float VOLATILE_BASE_POWER = 2.0F;
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final List<EquipmentSlot> EQUIPMENT = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    public static final TagKey<Item> GEODE_CRACKER_DROPS = TagKey.create(Registries.ITEM, CoreCrucible.id("geode_cracker_drops"));
    public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, CoreCrucible.id("ores"));

    /** Who receives (Ender's Grasp) and multiplies (Geode Cracker) the items spawned by the current break or death. */
    private record DropCapture(ServerPlayer player, int grasp, int geode) {
    }

    // Stacks, because breaking one block can break others inside it (vein mining): the newest break is on top.
    private static final Deque<DropCapture> CAPTURES = new ArrayDeque<>();
    private static final Deque<BlockState> BREAKING = new ArrayDeque<>();
    /** True while Vein Resonance or Tectonic Pulse break extra blocks, so they never chain off each other. */
    private static boolean cascading;

    /** How many hits in a row (Kinetic Resonance) a player has landed on the same target recently. */
    private static final class Streak {
        UUID target;
        int hits;
        long lastTick = Long.MIN_VALUE;
    }

    private static final Map<Player, Streak> STREAKS = new WeakHashMap<>();
    /** Arrows shot with Volatile Payload, and the explosion power they'll set off when they land. */
    private static final Map<AbstractArrow, Float> VOLATILE_ARROWS = new WeakHashMap<>();

    /** An explosion queued up to happen at the end of the tick (Volatile Payload). */
    private record Blast(ServerLevel level, Vec3 pos, Entity owner, float power) {
    }

    private static final List<Blast> PENDING_BLASTS = new ArrayList<>();
    /** Entities Soul Tether currently forbids from teleporting, by uuid, until this game tick. */
    private static final Map<UUID, Long> TETHERED = new HashMap<>();

    /** A Molten Tread magma block placed over lava, identified by dimension and position. */
    private record MagmaKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** Molten Tread magma blocks waiting to melt back into lava, and when. */
    private static final Map<MagmaKey, Long> TEMPORARY_MAGMA = new HashMap<>();

    /** A Phalanx Ward raised by a blocking player: which level it's in, when it fades, and its strength. */
    private record Ward(ServerLevel level, long expiry, int strength) {
    }

    private static final Map<Player, Ward> WARDS = new WeakHashMap<>();

    /** The client-only block displays Cavernous Echo spawned for one player, and when to remove them. */
    private record Outlines(UUID player, int[] ids, long expiry) {
    }

    private static final List<Outlines> OUTLINES = new ArrayList<>();
    private static final Map<UUID, Long> ECHO_COOLDOWN = new HashMap<>();

    private EnchantmentHooks() {
    }

    // ---- Level lookups --------------------------------------------------------------------------------------

    // Enchantments are data (JSON), so they're looked up in the world's registry rather than held as Java objects.
    private static Holder<Enchantment> holder(Level level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
    }

    /** Level on one item stack. */
    public static int level(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Holder<Enchantment> enchantment = holder(level, key);
        return enchantment == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

    /** Highest level across the slots the enchantment is active in. */
    public static int level(LivingEntity entity, ResourceKey<Enchantment> key) {
        Holder<Enchantment> enchantment = holder(entity.level(), key);
        return enchantment == null ? 0 : EnchantmentHelper.getEnchantmentLevel(enchantment, entity);
    }

    // ---- Block breaking: Vein Resonance, Tectonic Pulse, Luminous Ward, Geode Cracker, Ender's Grasp --------------

    /** Called at the start of {@code ServerPlayerGameMode.destroyBlock}. */
    // Remember who's breaking what, so drops spawned during the break can be handled (see onEntitySpawn).
    public static void onBeforeBreak(ServerPlayer player, BlockState state) {
        ItemStack tool = player.getMainHandItem();
        Level level = player.level();
        // No Geode Cracker on blocks with a block entity: a chest's spilled contents would otherwise be multiplied.
        int geode = state.hasBlockEntity() ? 0 : level(level, ModEnchantments.GEODE_CRACKER, tool);
        CAPTURES.push(new DropCapture(player, level(level, ModEnchantments.ENDERS_GRASP, tool), geode));
        BREAKING.push(state);
    }

    /** Called on every return from {@code ServerPlayerGameMode.destroyBlock}. */
    public static void onAfterBreak(ServerPlayer player, BlockPos pos, boolean broken) {
        CAPTURES.poll();
        BlockState state = BREAKING.poll();
        if (!broken || state == null || state.isAir() || cascading || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        // Extra breaks below call destroyBlock again, which comes back here; "cascading" stops that from looping.
        ItemStack tool = player.getMainHandItem();
        cascading = true;
        try {
            if (level(level, ModEnchantments.TECTONIC_PULSE, tool) > 0) {
                breakFallingColumn(player, pos, tool);
            }
            int vein = VeinMining.level(level, tool);
            VeinShape shape = vein > 0 ? VeinMining.activeShape(player, vein) : null;
            if (shape != null) {
                for (BlockPos extra : VeinMining.plan(level, player, pos, state, VeinMining.facing(player, pos), tool, vein, shape)) {
                    if (!canKeepMining(player, tool)) break;
                    player.gameMode.destroyBlock(extra);
                }
            }
        } finally {
            cascading = false;
        }
        if (level(level, ModEnchantments.LUMINOUS_WARD, tool) > 0
                && level.getMaxLocalRawBrightness(player.blockPosition()) < DARK && level.getBlockState(pos).isAir()) {
            level.setBlockAndUpdate(pos, ETHEREAL_LIGHT);
        }
    }

    /** Stops cascades before the tool would break (or once it has been swapped away). */
    // Leaves the tool on its last point of durability rather than breaking it.
    private static boolean canKeepMining(Player player, ItemStack tool) {
        return player.getMainHandItem() == tool && !tool.isEmpty()
                && (!tool.isDamageableItem() || tool.getMaxDamage() - tool.getDamageValue() > 1);
    }

    // Tectonic Pulse: break falling blocks above the broken one, up to 64 high.
    private static void breakFallingColumn(ServerPlayer player, BlockPos pos, ItemStack tool) {
        BlockPos above = pos.above();
        for (int i = 0; i < 64 && player.level().getBlockState(above).getBlock() instanceof FallingBlock && canKeepMining(player, tool); i++) {
            player.gameMode.destroyBlock(above);
            above = above.above();
        }
    }

    /** Called at the start of {@code LivingEntity.dropAllDeathLoot}. */
    public static void onBeforeDeathDrops(DamageSource source) {
        ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player : null;
        int grasp = killer == null ? 0 : level(killer.level(), ModEnchantments.ENDERS_GRASP, killer.getMainHandItem());
        CAPTURES.push(new DropCapture(killer, grasp, 0));
    }

    public static void onAfterDeathDrops() {
        CAPTURES.poll();
    }

    /**
     * Called from {@code ServerLevel.addFreshEntity}; returns true to cancel the spawn. Applies Geode Cracker and
     * Ender's Grasp to drops of the current break or death, and arms Volatile Payload arrows.
     */
    public static boolean onEntitySpawn(ServerLevel level, Entity entity) {
        if (entity instanceof AbstractArrow arrow) {
            armVolatileArrow(level, arrow);
            return false;
        }
        DropCapture capture = CAPTURES.peek();
        if (capture == null || !(entity instanceof ItemEntity item)) return false;
        ItemStack stack = item.getItem();
        // Geode Cracker: add 0 to (level) extra items.
        if (capture.geode() > 0 && stack.is(GEODE_CRACKER_DROPS)) {
            stack.grow(level.getRandom().nextInt(capture.geode() + 1));
        }
        ServerPlayer player = capture.player();
        // Ender's Grasp: put the drop in the player's inventory. Whatever doesn't fit is moved to the player's feet.
        if (capture.grasp() > 0 && player != null && player.isAlive()) {
            player.getInventory().add(stack);
            if (stack.isEmpty()) return true;
            item.setPos(player.getX(), player.getY(), player.getZ());
            item.setNoPickUpDelay();
        }
        item.setItem(stack);
        return false;
    }

    // ---- Combat ---------------------------------------------------------------------------------------------

    /** Called with the result of {@code EnchantmentHelper.modifyDamage}: Kinetic Resonance. */
    public static float modifyAttackDamage(ServerLevel level, ItemStack weapon, Entity victim, DamageSource source, float damage) {
        if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) return damage;
        int kinetic = level(level, ModEnchantments.KINETIC_RESONANCE, weapon);
        return kinetic > 0 ? damage * (1.0F + kineticBonus(level, player, victim, kinetic)) : damage;
    }

    /** +10% per consecutive hit on the same target within 3 seconds, capped at +30/40/50%. */
    private static float kineticBonus(ServerLevel level, Player player, Entity victim, int enchantmentLevel) {
        long now = level.getGameTime();
        Streak streak = STREAKS.computeIfAbsent(player, p -> new Streak());
        boolean sameTarget = victim.getUUID().equals(streak.target);
        if (streak.lastTick == now && !sameTarget) return 0.0F; // a sweep hitting a bystander in the same swing
        // 60 ticks = 3 seconds. A different target or a slow hit starts the streak again.
        streak.hits = sameTarget && now - streak.lastTick <= 60 ? streak.hits + 1 : 0;
        streak.target = victim.getUUID();
        streak.lastTick = now;
        return Math.min(0.1F * streak.hits, 0.2F + 0.1F * enchantmentLevel);
    }

    /** Before damage is applied; returns true to cancel it (Molten Tread, Phalanx Ward). */
    public static boolean onLivingHurt(ServerLevel level, LivingEntity victim, DamageSource source) {
        // Molten Tread: magma's floor heat doesn't hurt the wearer.
        if (source.is(DamageTypes.HOT_FLOOR) && level(victim, ModEnchantments.MOLTEN_TREAD) > 0) return true;
        // Volatile Payload: an armed arrow that hits a mob explodes (at the end of the tick).
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            Float power = VOLATILE_ARROWS.remove(arrow);
            if (power != null) PENDING_BLASTS.add(new Blast(level, arrow.position(), arrow.getOwner(), power));
        }
        return phalanxIntercepts(level, victim, source);
    }

    /** After damage is applied, or after a shield blocked it. */
    public static void onLivingDamaged(ServerLevel level, LivingEntity victim, DamageSource source, boolean blocked) {
        if (blocked) {
            if (victim instanceof Player defender) raisePhalanxWard(level, defender);
            return;
        }
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && attacker != victim) {
            ItemStack weapon = attacker.getMainHandItem();
            // Soul Tether: stop the victim teleporting for TETHER_TICKS.
            if (level(level, ModEnchantments.SOUL_TETHER, weapon) > 0) {
                TETHERED.put(victim.getUUID(), level.getGameTime() + TETHER_TICKS);
                level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY(0.5), victim.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
            }
            if (attacker instanceof ServerPlayer player && !player.isCreative()
                    && level(level, ModEnchantments.SOUL_SYPHON, weapon) > 0) {
                // The weapon takes no durability damage (see soul_syphon.json); its wielder pays instead.
                if (player.totalExperience >= 2) {
                    player.giveExperiencePoints(-2);
                } else {
                    //? if >=1.21.2 {
                    player.hurtServer(level, level.damageSources().magic(), 1.0F);
                    //?} else {
                    /*player.hurt(level.damageSources().magic(), 1.0F);
                    *///?}
                }
            }
        }
        if (victim instanceof ServerPlayer player) {
            // Cavernous Echo: a player getting hurt checks for ores around them.
            int echo = level(player, ModEnchantments.CAVERNOUS_ECHO);
            if (echo > 0) cavernousEcho(level, player, echo);
        }
    }

    // ---- Phalanx Ward ---------------------------------------------------------------------------------------

    private static InteractionHand phalanxHand(Level level, Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (level(level, ModEnchantments.PHALANX_WARD, player.getItemInHand(hand)) > 0) return hand;
        }
        return null;
    }

    // Phalanx Ward starts when the player blocks a hit: it lasts 3 seconds (60 ticks) per level.
    private static void raisePhalanxWard(ServerLevel level, Player defender) {
        InteractionHand hand = phalanxHand(level, defender);
        if (hand == null) return;
        int enchantmentLevel = level(level, ModEnchantments.PHALANX_WARD, defender.getItemInHand(hand));
        WARDS.put(defender, new Ward(level, level.getGameTime() + 60L * enchantmentLevel, enchantmentLevel));
        // A 3x3 barrier of light in front of the shield
        Vec3 look = defender.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        Vec3 side = new Vec3(-look.z, 0.0, look.x);
        Vec3 centre = defender.position().add(look.scale(1.5)).add(0.0, 1.0, 0.0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Vec3 p = centre.add(side.scale(dx)).add(0.0, dy, 0.0);
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.0);
            }
        }
    }

    // Called before anyone takes damage. If a warding player is close enough and the victim is on their side (team,
    // their pet, or a player hit by a mob), the hit is cancelled and the ward's shield loses 1 durability.
    private static boolean phalanxIntercepts(ServerLevel level, LivingEntity victim, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == null || WARDS.isEmpty()) return false;
        long now = level.getGameTime();
        for (Map.Entry<Player, Ward> entry : WARDS.entrySet()) {
            Player defender = entry.getKey();
            Ward ward = entry.getValue();
            if (defender == victim || defender == attacker || ward.level() != level || ward.expiry() < now || !defender.isAlive()) {
                continue;
            }
            double range = 3.0 + ward.strength();
            boolean ally = victim.isAlliedTo(defender)
                    || victim instanceof OwnableEntity pet && pet.getOwner() == defender
                    || victim instanceof Player && !(attacker instanceof Player);
            if (!ally || victim.distanceToSqr(defender) > range * range) continue;
            InteractionHand hand = phalanxHand(level, defender);
            if (hand == null) continue;
            //? if >=1.21.2 {
            defender.getItemInHand(hand).hurtAndBreak(1, defender, hand.asEquipmentSlot());
            level.playSound(null, victim.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
            //?} else {
            /*defender.getItemInHand(hand).hurtAndBreak(1, defender, LivingEntity.getSlotForHand(hand));
            level.playSound(null, victim.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
            *///?}
            level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY(0.5), victim.getZ(), 6, 0.4, 0.5, 0.4, 0.02);
            return true;
        }
        return false;
    }

    // ---- Aegis Reflection, Gravitic Anchor, Soul Tether (called from mixins) -----------------------------------

    /** Whether {@code defender} turns {@code projectile} back (always while blocking with an Aegis shield). */
    public static boolean aegisReflects(LivingEntity defender, Projectile projectile) {
        if (!(defender.level() instanceof ServerLevel level) || projectile.getOwner() == defender) return false;
        if (defender.isBlocking() && level(level, ModEnchantments.AEGIS_REFLECTION, defender.getUseItem()) > 0) return true;
        int enchantmentLevel = level(defender, ModEnchantments.AEGIS_REFLECTION);
        return enchantmentLevel > 0 && level.getRandom().nextFloat() < 0.15F * enchantmentLevel;
    }

    public static boolean hasGraviticAnchor(LivingEntity entity) {
        return level(entity, ModEnchantments.GRAVITIC_ANCHOR) > 0;
    }

    /** Whether Soul Tether currently forbids {@code entity} from teleporting (server side only). */
    public static boolean isTethered(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        Long expiry = TETHERED.get(entity.getUUID());
        boolean tethered = expiry != null && expiry > level.getGameTime();
        if (tethered && entity instanceof Player player) {
            Component message = Component.translatable("message." + CoreCrucible.MOD_ID + ".tethered");
            //? if >=1.21.2 {
            player.sendOverlayMessage(message);
            //?} else {
            /*player.displayClientMessage(message, true);
            *///?}
        }
        return tethered;
    }

    // ---- Volatile Payload -----------------------------------------------------------------------------------

    private static void armVolatileArrow(ServerLevel level, AbstractArrow arrow) {
        ItemStack weapon = arrow.getWeaponItem();
        // Arrows from a Volatile Payload launcher are remembered with their explosion power. Bigger levels = bigger blast.
        if (weapon == null || !arrow.isCritArrow() || !(arrow.getOwner() instanceof LivingEntity)) return; // only fully drawn shots
        int enchantmentLevel = level(level, ModEnchantments.VOLATILE_PAYLOAD, weapon);
        if (enchantmentLevel > 0) VOLATILE_ARROWS.put(arrow, VOLATILE_BASE_POWER * (0.5F + enchantmentLevel / 6.0F));
    }

    // Every tick: arrows that stuck in a block explode, then every queued explosion goes off. NONE = no block damage.
    private static void tickVolatileArrows() {
        Iterator<Map.Entry<AbstractArrow, Float>> it = VOLATILE_ARROWS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<AbstractArrow, Float> entry = it.next();
            AbstractArrow arrow = entry.getKey();
            if (arrow.isRemoved()) {
                it.remove();
            } else if (((AbstractArrowAccessor) arrow).dooles_core_crucible$inGround() && arrow.level() instanceof ServerLevel level) {
                it.remove();
                PENDING_BLASTS.add(new Blast(level, arrow.position(), arrow.getOwner(), entry.getValue()));
                arrow.discard();
            }
        }
        List<Blast> blasts = new ArrayList<>(PENDING_BLASTS);
        PENDING_BLASTS.clear();
        for (Blast blast : blasts) {
            blast.level().explode(blast.owner(), blast.pos().x, blast.pos().y, blast.pos().z, blast.power(), Level.ExplosionInteraction.NONE);
        }
    }

    // ---- Cavernous Echo -------------------------------------------------------------------------------------

    /** Outlines nearby ores for this player only, using client-side glowing block displays that never exist on the server. */
    private static void cavernousEcho(ServerLevel level, ServerPlayer player, int enchantmentLevel) {
        long now = level.getGameTime();
        BlockPos centre = player.blockPosition();
        if (level.getMaxLocalRawBrightness(centre) >= DARK || ECHO_COOLDOWN.getOrDefault(player.getUUID(), Long.MIN_VALUE) > now) {
            return;
        }
        ECHO_COOLDOWN.put(player.getUUID(), now + ECHO_TICKS);

        int radius = 8 * enchantmentLevel;
        List<BlockPos> ores = new ArrayList<>();
        // Find ores (the mod's ores tag) in a cube around the player, nearest first, up to ECHO_MAX_ORES.
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(ORES)) ores.add(pos.immutable());
        }
        ores.sort(Comparator.comparingDouble(pos -> pos.distSqr(centre)));

        int count = Math.min(ores.size(), ECHO_MAX_ORES);
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            BlockPos pos = ores.get(i);
            //? if >=1.21.2 {
            Display.BlockDisplay display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);
            //?} else {
            /*Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
            *///?}
            ((BlockDisplayAccessor) display).dooles_core_crucible$setBlockState(level.getBlockState(pos));
            ((DisplayAccessor) display).dooles_core_crucible$setTransformation(new Transformation(new Vector3f(0.02F), null, new Vector3f(0.96F), null));
            ((DisplayAccessor) display).dooles_core_crucible$setGlowColorOverride(0xFFD27F);
            display.setGlowingTag(true);
            display.setPos(pos.getX(), pos.getY(), pos.getZ());
            ids[i] = display.getId();
            // The glowing blocks are only sent to this player's client. They never exist on the server, so nobody else sees them
            // and nothing is saved.
            player.connection.send(new ClientboundAddEntityPacket(display, 0, pos));
            List<SynchedEntityData.DataValue<?>> data = display.getEntityData().getNonDefaultValues();
            if (data != null) player.connection.send(new ClientboundSetEntityDataPacket(display.getId(), data));
        }
        if (count > 0) {
            OUTLINES.add(new Outlines(player.getUUID(), ids, now + ECHO_TICKS));
            level.playSound(null, centre, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    // Removes each player's glowing ore outlines when their time runs out.
    private static void tickOutlines(MinecraftServer server, long now) {
        Iterator<Outlines> it = OUTLINES.iterator();
        while (it.hasNext()) {
            Outlines outlines = it.next();
            if (outlines.expiry() <= now) {
                it.remove();
                ServerPlayer player = server.getPlayerList().getPlayer(outlines.player());
                if (player != null) player.connection.send(new ClientboundRemoveEntitiesPacket(outlines.ids()));
            }
        }
    }

    // ---- Ticking: Luminous Ward, Thermal Tempering, Molten Tread --------------------------------------------------

    public static void onPlayerTick(ServerLevel level, Player player) {
        if (player.isSpectator()) return;
        moltenTread(level, player);
        if (player.tickCount % 10 == 0) luminousArmor(level, player);
        if (player.tickCount % 20 == 0) thermalTempering(level, player);
    }

    // Luminous Ward on armor: in the dark, place an invisible light block at the player's feet.
    private static void luminousArmor(ServerLevel level, Player player) {
        BlockPos feet = player.blockPosition();
        if (!level.getBlockState(feet).isAir() || level.getMaxLocalRawBrightness(feet) >= DARK) return;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack piece = player.getItemBySlot(slot);
            if (level(level, ModEnchantments.LUMINOUS_WARD, piece) > 0) {
                level.setBlockAndUpdate(feet, ETHEREAL_LIGHT);
                piece.hurtAndBreak(1, player, slot);
                return;
            }
        }
    }

    // Runs once a second (see onPlayerTick).
    private static void thermalTempering(ServerLevel level, Player player) {
        boolean hot = player.isOnFire() || player.isInLava() || player.getBlockStateOn().is(Blocks.MAGMA_BLOCK);
        if (!hot) return;
        for (EquipmentSlot slot : EQUIPMENT) {
            ItemStack stack = player.getItemBySlot(slot);
            int enchantmentLevel = level(level, ModEnchantments.THERMAL_TEMPERING, stack);
            if (enchantmentLevel > 0 && stack.isDamaged()) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 2 * enchantmentLevel));
            }
        }
    }

    // Turns lava source blocks in a circle under the player into magma, and schedules them to melt back after 5-8 seconds.
    private static void moltenTread(ServerLevel level, Player player) {
        int enchantmentLevel = level(level, ModEnchantments.MOLTEN_TREAD, player.getItemBySlot(EquipmentSlot.FEET));
        if (enchantmentLevel <= 0 || !player.onGround()) return;
        int radius = 1 + enchantmentLevel;
        BlockPos below = player.blockPosition().below();
        long now = level.getGameTime();
        for (BlockPos pos : BlockPos.betweenClosed(below.offset(-radius, 0, -radius), below.offset(radius, 0, radius))) {
            if (pos.distSqr(below) > radius * radius) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.LAVA) && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()) {
                BlockPos immutable = pos.immutable();
                level.setBlockAndUpdate(immutable, Blocks.MAGMA_BLOCK.defaultBlockState());
                TEMPORARY_MAGMA.put(new MagmaKey(level.dimension(), immutable), now + 100 + level.getRandom().nextInt(60));
            }
        }
    }

    // Melts Molten Tread's magma back into lava when its time is up, unless someone with Molten Tread is still standing
    // near it. force = melt everything now (server shutting down).
    private static void tickTemporaryMagma(MinecraftServer server, boolean force) {
        Iterator<Map.Entry<MagmaKey, Long>> it = TEMPORARY_MAGMA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MagmaKey, Long> entry = it.next();
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            if (level == null) {
                it.remove();
                continue;
            }
            long now = level.getGameTime();
            if (!force && entry.getValue() > now) continue;
            BlockPos pos = entry.getKey().pos();
            if (!force && !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.5, 1.0, 1.5),
                    entity -> level(level, ModEnchantments.MOLTEN_TREAD, entity.getItemBySlot(EquipmentSlot.FEET)) > 0).isEmpty()) {
                entry.setValue(now + 20); // still being walked on
                continue;
            }
            if (level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
            it.remove();
        }
    }

    // ---- Server lifecycle -----------------------------------------------------------------------------------

    public static void onServerTick(MinecraftServer server) {
        tickVolatileArrows();
        tickTemporaryMagma(server, false);
        long now = server.overworld().getGameTime();
        tickOutlines(server, now);
        // Every 10 seconds, forget expired entries so the maps don't grow forever.
        if (now % 200 == 0) {
            TETHERED.values().removeIf(expiry -> expiry <= now);
            ECHO_COOLDOWN.values().removeIf(expiry -> expiry <= now);
            WARDS.values().removeIf(ward -> ward.expiry() <= ward.level().getGameTime());
            VeinMining.cleanUp(server);
        }
    }

    /** Turns every temporary magma block back into lava so none are saved into the world. */
    public static void onServerStopping(MinecraftServer server) {
        tickTemporaryMagma(server, true);
        TEMPORARY_MAGMA.clear();
        VOLATILE_ARROWS.clear();
        PENDING_BLASTS.clear();
        TETHERED.clear();
        OUTLINES.clear();
        ECHO_COOLDOWN.clear();
        WARDS.clear();
        STREAKS.clear();
        CAPTURES.clear();
        BREAKING.clear();
        VeinMining.clear();
    }
}
