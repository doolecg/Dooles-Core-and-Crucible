package dev.doole.corecrucible.platform.neoforge;

//? neoforge {

/*import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.gameplay.TemplateLoot;
import dev.doole.corecrucible.platform.Events;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

// Listens for NeoForge's game events (damage, loot table loading, server stopping) and forwards them to the
// shared Events class, the same way Fabric's own events do in FabricEntrypoint.
@EventBusSubscriber(modid = CoreCrucible.MOD_ID)
public class NeoforgeGameEvents {

    // A living entity just took damage.
    @SubscribeEvent
    private static void onLivingDamaged(LivingDamageEvent.Post event) {
        //? if >=1.21.2 {
        float damage = event.getInflictedDamage();
        //?} else {
        /^float damage = event.getNewDamage();
        ^///?}
        Events.livingDamaged(event.getEntity(), event.getSource(), damage, false);
    }

    // A shield blocked a hit instead of the entity taking damage.
    @SubscribeEvent
    private static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked()) Events.livingDamaged(event.getEntity(), event.getDamageSource(), 0, true);
    }

    // A vanilla loot table just loaded: add the mod's bonus pool if TemplateLoot targets it.
    @SubscribeEvent
    private static void onLootTableLoad(LootTableLoadEvent event) {
        if (!TemplateLoot.isTarget(event.getKey())) return;
        event.getTable().addPool(TemplateLoot.pool(event.getKey()).name(CoreCrucible.MOD_ID + ":iron_template").build());
    }

    @SubscribeEvent
    private static void onServerStopping(ServerStoppingEvent event) {
        Events.serverStopping(event.getServer());
    }
}
*///?}
