package com.beanz.core.spike;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * BS0 spike piece 5: a menu whose buttons the server reacts to.
 * "Bind" adds an entry to the saved per-player list (piece 4), so one click proves menu + save together. Throwaway.
 */
public class SpikeMenuPage extends InteractiveCustomUIPage<SpikeMenuPage.Data> {
    private static final String LAYOUT = "Pages/BeanzSpikeMenu.ui";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static class Data {
        public static final BuilderCodec<Data> CODEC = BuilderCodec
            .builder(Data.class, Data::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .add()
            .build();

        String action;
    }

    private int clicks;

    public SpikeMenuPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        ui.append(LAYOUT);
        ui.set("#Status.Text", statusText(store, ref));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BindButton", EventData.of("Action", "bind"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ClearButton", EventData.of("Action", "clear"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, Data data) {
        String action = data != null ? data.action : null;
        LOGGER.atInfo().log("[BeanzCore][Spike] menu click action=%s player=%s", action, playerRef.getUsername());
        if ("close".equals(action)) {
            close();
            return;
        }
        SpikeListComponent list = store.ensureAndGetComponent(ref, SpikeListComponent.getComponentType());
        if ("bind".equals(action)) {
            clicks++;
            list.getEntries().add("Head-" + (list.getEntries().size() + 1) + ": Test Shard");
        } else if ("clear".equals(action)) {
            list.getEntries().clear();
        }
        UICommandBuilder update = new UICommandBuilder();
        update.set("#Status.Text", statusText(store, ref));
        sendUpdate(update, false);
    }

    private String statusText(Store<EntityStore> store, Ref<EntityStore> ref) {
        SpikeListComponent list = store.getComponent(ref, SpikeListComponent.getComponentType());
        int saved = list != null ? list.getEntries().size() : 0;
        String last = saved > 0 ? list.getEntries().get(saved - 1) : "none";
        return "Clicks this visit: " + clicks + "   |   Saved entries: " + saved + "   |   Last: " + last;
    }
}
