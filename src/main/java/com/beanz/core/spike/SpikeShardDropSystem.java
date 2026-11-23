package com.beanz.core.spike;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Locale;
import org.joml.Vector3d;

/**
 * BS0 spike piece 1: when a mob from one of the first-slice families dies, drop a test shard.
 * Done from code (not by editing the game's drop tables) so the mapping can later come from data. Throwaway.
 */
public class SpikeShardDropSystem extends DeathSystems.OnDeathSystem {
    public static final String SHARD_ITEM_ID = "Beanz_Spike_Shard";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Test switch (/spike dropall): drop a shard from every mob, not just the four families. */
    static volatile boolean dropForAnyMob;

    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void onComponentAdded(
        Ref<EntityStore> ref,
        DeathComponent death,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> buffer
    ) {
        NPCEntity npc = buffer.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }
        String role = npc.getRoleName();
        String family = familyOf(role);
        if (family == null && dropForAnyMob) {
            family = "AnyMob";
        }
        LOGGER.atInfo().log("[BeanzCore][Spike] NPC died: role=%s family=%s", role, family);
        if (family == null) {
            return;
        }
        Vector3d pos = SpikeFx.position(ref, buffer);
        if (pos == null) {
            return;
        }
        Holder<EntityStore> drop = ItemComponent.generateItemDrop(
            buffer,
            new ItemStack(SHARD_ITEM_ID, 1),
            new Vector3d(pos.x(), pos.y() + 0.5, pos.z()),
            new Rotation3f(),
            0.0f,
            0.25f,
            0.0f
        );
        if (drop != null) {
            buffer.addEntity(drop, AddReason.SPAWN);
            LOGGER.atInfo().log("[BeanzCore][Spike] dropped %s for %s (%s)", SHARD_ITEM_ID, role, family);
        }
    }

    /** First-slice families from SHARDS-PLAN.md BS4. Returns null for anything else. */
    static String familyOf(String role) {
        if (role == null) {
            return null;
        }
        String r = role.toLowerCase(Locale.ROOT);
        if (r.startsWith("trork")) {
            return "Greenskin";
        }
        if (r.startsWith("skeleton")) {
            return "Hollow";
        }
        if (r.startsWith("spider")) {
            return "Crawler";
        }
        if (r.startsWith("wolf") || r.startsWith("bear")) {
            return "Beast";
        }
        return null;
    }
}
