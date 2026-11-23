package com.beanz.core.spike;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.AndQuery;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.Arrays;
import org.joml.Vector3d;

/**
 * BS0 spike test command. Every piece of the spike can be poked from chat with /spike. Throwaway.
 */
public class SpikeCommand extends AbstractPlayerCommand {
    public static final String FOCUS_ITEM_ID = "Beanz_Spike_Focus";
    private static final double NEAR_RANGE = 12.0;

    public SpikeCommand() {
        super("spike", "Shards BS0 spike tests (/spike help)");
        setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        String input = context.getInputString();
        String[] all = input != null ? input.trim().split("\\s+") : new String[0];
        String[] args = all.length > 1 ? Arrays.copyOfRange(all, 1, all.length) : new String[0];
        String sub = args.length > 0 ? args[0].toLowerCase() : "help";

        String reply = switch (sub) {
            case "menu" -> openMenu(store, ref, playerRef);
            case "give" -> give(args, store, ref);
            case "fx" -> args.length > 1 ? SpikeFx.particle(args[1], ref, store) : "usage: /spike fx <ParticleSystemId>";
            case "sound" -> args.length > 1 ? SpikeFx.sound(args[1], ref, store) : "usage: /spike sound <SoundEventId>";
            case "effect" -> args.length > 1 ? SpikeFx.effect(args[1], seconds(args), ref, ref, store) : "usage: /spike effect <EffectId> [seconds]";
            case "near" -> args.length > 1 ? near(args, store, ref) : "usage: /spike near <EffectId> [seconds]";
            case "list" -> listText(store, ref);
            case "add" -> add(args, store, ref);
            case "clear" -> {
                store.ensureAndGetComponent(ref, SpikeListComponent.getComponentType()).getEntries().clear();
                yield "saved list cleared";
            }
            case "dropall" -> {
                SpikeShardDropSystem.dropForAnyMob = args.length < 2 || !"off".equalsIgnoreCase(args[1]);
                yield "shard drops from every mob: " + (SpikeShardDropSystem.dropForAnyMob ? "ON" : "OFF");
            }
            default -> "/spike menu | give shard|focus | fx <id> | sound <id> | effect <id> [s] | near <id> [s] | list | add <text> | clear | dropall [off]";
        };
        context.sendMessage(Message.raw("[Spike] " + reply));
    }

    private String openMenu(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return "no player component";
        }
        player.getPageManager().openCustomPage(ref, store, new SpikeMenuPage(playerRef));
        return "menu opened";
    }

    private String give(String[] args, Store<EntityStore> store, Ref<EntityStore> ref) {
        String what = args.length > 1 ? args[1].toLowerCase() : "shard";
        String itemId = "focus".equals(what) ? FOCUS_ITEM_ID : SpikeShardDropSystem.SHARD_ITEM_ID;
        Player.giveItem(new ItemStack(itemId, 1), ref, store);
        return "gave " + itemId;
    }

    private String add(String[] args, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (args.length < 2) {
            return "usage: /spike add <text>";
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        store.ensureAndGetComponent(ref, SpikeListComponent.getComponentType()).getEntries().add(text);
        return "added '" + text + "'. " + listText(store, ref);
    }

    private String listText(Store<EntityStore> store, Ref<EntityStore> ref) {
        SpikeListComponent list = store.getComponent(ref, SpikeListComponent.getComponentType());
        if (list == null || list.getEntries().isEmpty()) {
            return "saved list is empty";
        }
        return "saved list (" + list.getEntries().size() + "): " + String.join(" / ", list.getEntries());
    }

    private String near(String[] args, Store<EntityStore> store, Ref<EntityStore> ref) {
        Vector3d me = SpikeFx.position(ref, store);
        if (me == null) {
            return "no position";
        }
        final Ref<EntityStore>[] best = new Ref[1];
        final double[] bestDist = {NEAR_RANGE * NEAR_RANGE};
        final String[] bestRole = {""};
        store.forEachChunk(new AndQuery<>(NPCEntity.getComponentType(), TransformComponent.getComponentType()), (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                TransformComponent t = chunk.getComponent(i, TransformComponent.getComponentType());
                if (t == null || t.getPosition() == null) {
                    continue;
                }
                double d = t.getPosition().distanceSquared(me);
                if (d < bestDist[0]) {
                    bestDist[0] = d;
                    best[0] = chunk.getReferenceTo(i);
                    NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                    bestRole[0] = npc != null ? npc.getRoleName() : "?";
                }
            }
        });
        if (best[0] == null) {
            return "no mob within " + (int) NEAR_RANGE + " blocks";
        }
        return bestRole[0] + ": " + SpikeFx.effect(args[1], seconds(args), best[0], ref, store);
    }

    private static float seconds(String[] args) {
        if (args.length < 3) {
            return 0f;
        }
        try {
            return Float.parseFloat(args[2]);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
