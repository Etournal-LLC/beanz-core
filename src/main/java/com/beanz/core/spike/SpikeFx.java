package com.beanz.core.spike;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import org.joml.Vector3d;

/**
 * BS0 spike pieces 3 and 6: built-in particles, sounds and entity effects played from code.
 * Each method returns a short result line for chat. Throwaway.
 */
public final class SpikeFx {
    private SpikeFx() {
    }

    /** Finds an asset id ignoring case, so chat typos like "impact_fire" still work. Null if there is no such asset. */
    private static String resolve(String id, Map<String, ?> assets) {
        if (assets.containsKey(id)) {
            return id;
        }
        for (String key : assets.keySet()) {
            if (key.equalsIgnoreCase(id)) {
                return key;
            }
        }
        return null;
    }

    public static String particle(String typedId, Ref<EntityStore> at, ComponentAccessor<EntityStore> accessor) {
        String systemId = resolve(typedId, ParticleSystem.getAssetMap().getAssetMap());
        if (systemId == null) {
            return "unknown particle '" + typedId + "'";
        }
        Vector3d pos = position(at, accessor);
        if (pos == null) {
            return "no position for target";
        }
        ParticleUtil.spawnParticleEffect(systemId, new Vector3d(pos.x(), pos.y() + 1.0, pos.z()), accessor);
        return "particle '" + systemId + "' sent";
    }

    public static String sound(String typedId, Ref<EntityStore> at, ComponentAccessor<EntityStore> accessor) {
        String soundEventId = resolve(typedId, SoundEvent.getAssetMap().getAssetMap());
        if (soundEventId == null) {
            return "unknown sound event '" + typedId + "'";
        }
        int index = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (index < 0) {
            return "unknown sound event '" + soundEventId + "'";
        }
        Vector3d pos = position(at, accessor);
        if (pos == null) {
            return "no position for target";
        }
        SoundUtil.playSoundEvent3d(index, SoundCategory.SFX, pos, accessor);
        return "sound '" + soundEventId + "' (index " + index + ") played";
    }

    /** Applies a built-in or mod-authored EntityEffect. seconds <= 0 uses the asset's own duration. */
    public static String effect(
        String effectId,
        float seconds,
        Ref<EntityStore> target,
        Ref<EntityStore> source,
        ComponentAccessor<EntityStore> accessor
    ) {
        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
        if (effect == null) {
            return "unknown effect '" + effectId + "'";
        }
        EffectControllerComponent controller = accessor.getComponent(target, EffectControllerComponent.getComponentType());
        if (controller == null) {
            return "target has no effect controller";
        }
        boolean applied = seconds > 0
            ? controller.addEffect(target, effect, seconds, OverlapBehavior.OVERWRITE, accessor, source)
            : controller.addEffect(target, effect, accessor, source);
        return "effect '" + effectId + "' " + (applied ? "applied" : "was rejected") + (seconds > 0 ? " for " + seconds + "s" : "");
    }

    public static Vector3d position(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        TransformComponent transform = accessor.getComponent(ref, TransformComponent.getComponentType());
        return transform != null ? transform.getPosition() : null;
    }
}
