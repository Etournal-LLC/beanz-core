package com.beanz.core.spike;

import com.google.common.flogger.FluentLogger;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * BS0 spike piece 2: one interaction type, wired to several keys on the Shard Focus item.
 * Each key's asset sets "Slot" so the chat line says which key actually fired. Throwaway.
 */
public class SpikeCastInteraction extends SimpleInstantInteraction {
    public static final String TYPE_ID = "Beanz_Spike_Cast";

    public static final BuilderCodec<SpikeCastInteraction> CODEC = BuilderCodec
        .builder(SpikeCastInteraction.class, SpikeCastInteraction::new, SimpleInstantInteraction.CODEC)
        .append(new KeyedCodec<>("Slot", Codec.STRING), (i, v) -> i.slot = v, i -> i.slot)
        .add()
        .append(new KeyedCodec<>("Particle", Codec.STRING), (i, v) -> i.particle = v, i -> i.particle)
        .add()
        .build();

    private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

    private String slot = "?";
    private String particle = "";

    public SpikeCastInteraction() {
        super(TYPE_ID);
    }

    public SpikeCastInteraction(String id) {
        super(id);
    }

    @Override
    protected void firstRun(InteractionType interactionType, InteractionContext context, CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> buffer = context.getCommandBuffer();
        Player player = buffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = player != null ? player.getPlayerRef() : null;

        MovementStatesComponent movement = buffer.getComponent(ref, MovementStatesComponent.getComponentType());
        boolean crouching = movement != null && movement.getMovementStates() != null && movement.getMovementStates().crouching;

        String line = "[Spike] key fired: slot=" + slot + " input=" + interactionType + (crouching ? " CROUCHING (second ability)" : " standing");
        LOGGER.atInfo().log("[BeanzCore][Spike] %s player=%s", line, playerRef != null ? playerRef.getUsername() : "?");
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(line));
        }
        String shown = crouching ? "Impact_Poison" : particle;
        if (shown != null && !shown.isEmpty()) {
            SpikeFx.particle(shown, ref, buffer);
        }
    }
}
