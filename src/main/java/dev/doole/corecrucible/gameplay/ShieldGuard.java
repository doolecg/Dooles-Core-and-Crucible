package dev.doole.corecrucible.gameplay;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.registry.ShieldStats;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.2 {
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//?} else {
/*import net.minecraft.world.entity.projectile.AbstractArrow;
*///?}

/**
 * The shield stats that aren't vanilla fields: knockback resistance while blocking and partial cover from
 * the side, on every version, plus the raise and axe-disable times that 1.21.1 hard-codes (26.x reads them from the
 * shield's {@code blocks_attacks} component instead).
 */
public final class ShieldGuard {
    private static final Identifier KNOCKBACK_ID = CoreCrucible.id("shield_guard");
    /** Hits up to this far off the front count as side hits; vanilla blocks everything up to 90°. */
    public static final double SIDE_ANGLE = Math.toRadians(135.0);

    private ShieldGuard() {
    }

    /** 1.21.1: ticks from raising {@code useItem} until it blocks; {@code vanilla} for anything that isn't a chain shield. */
    public static int raiseTicks(ItemStack useItem, int vanilla) {
        ShieldStats stats = ShieldStats.of(useItem);
        return stats == null ? vanilla : stats.raiseTicks();
    }

    /** 1.21.1: cooldown ticks after an axe knocks {@code useItem} down. */
    public static int disableTicks(ItemStack useItem, int vanilla) {
        ShieldStats stats = ShieldStats.of(useItem);
        return stats == null ? vanilla : (int) Math.round(stats.disableSeconds() * 20.0);
    }

    /**
     * The damage left after a raised shield's side cover: a hit from between 90° and {@link #SIDE_ANGLE} off where the
     * victim faces loses the tier's share. Front hits are vanilla's (fully blocked) and back hits aren't covered.
     */
    public static float sideCover(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0.0F || source.is(DamageTypeTags.BYPASSES_SHIELD) || !victim.isBlocking()) return amount;
        ShieldStats stats = ShieldStats.of(victim.getUseItem());
        if (stats == null || stats.sideCover() <= 0.0) return amount;
        // Piercing arrows go through shields in vanilla, so they go through the side cover too.
        if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return amount;
        Vec3 from = source.getSourcePosition();
        if (from == null) return amount;
        double angle = angleOff(victim.getYHeadRot(), from.x - victim.getX(), from.z - victim.getZ());
        // 0-90° is the front (vanilla blocks it fully), past SIDE_ANGLE is the back (no cover).
        if (angle <= Math.PI / 2 || angle > SIDE_ANGLE) return amount;
        return (float) (amount * (1.0 - stats.sideCover()));
    }

    /**
     * The horizontal angle (radians, 0 to π) between where an entity with head yaw {@code yawDegrees} faces and the
     * direction {@code (dx, dz)} towards the source. Minecraft's yaw 0 faces +Z and grows clockwise seen from above.
     */
    public static double angleOff(float yawDegrees, double dx, double dz) {
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-6) return 0.0;
        double yaw = Math.toRadians(yawDegrees);
        // Dot product of the facing direction (-sin yaw, cos yaw) with the normalised direction to the source; acos gives the angle.
        double dot = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / length;
        return Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
    }

    /** Every player tick (server side): adds the shield's knockback resistance while blocking, removes it after. */
    public static void tick(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        ShieldStats stats = player.isBlocking() ? ShieldStats.of(player.getUseItem()) : null;
        double amount = stats == null ? 0.0 : stats.knockbackResistance();
        // Only touch the attribute when the amount changes, rather than every tick. Transient = not saved with the player.
        AttributeModifier current = attribute.getModifier(KNOCKBACK_ID);
        if (current != null && current.amount() == amount) return;
        if (current != null) attribute.removeModifier(KNOCKBACK_ID);
        if (amount > 0.0) attribute.addTransientModifier(new AttributeModifier(KNOCKBACK_ID, amount, AttributeModifier.Operation.ADD_VALUE));
    }
}
