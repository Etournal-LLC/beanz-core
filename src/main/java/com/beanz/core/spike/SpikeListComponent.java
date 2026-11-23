package com.beanz.core.spike;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;

/**
 * BS0 spike piece 4: a per-player list saved with the player.
 * Stands in for the 24-slot shard body. Throwaway.
 */
public class SpikeListComponent implements Component<EntityStore> {
    public static final BuilderCodec<SpikeListComponent> CODEC = BuilderCodec
        .builder(SpikeListComponent.class, SpikeListComponent::new)
        .append(new KeyedCodec<>("Entries", Codec.STRING_ARRAY), SpikeListComponent::setEntriesArray, SpikeListComponent::getEntriesArray)
        .add()
        .build();

    private static ComponentType<EntityStore, SpikeListComponent> componentType;

    private final List<String> entries = new ArrayList<>();

    public static ComponentType<EntityStore, SpikeListComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, SpikeListComponent> type) {
        componentType = type;
    }

    public List<String> getEntries() {
        return entries;
    }

    public String[] getEntriesArray() {
        return entries.toArray(new String[0]);
    }

    public void setEntriesArray(String[] values) {
        entries.clear();
        if (values != null) {
            entries.addAll(List.of(values));
        }
    }

    @Override
    public SpikeListComponent clone() {
        SpikeListComponent copy = new SpikeListComponent();
        copy.entries.addAll(entries);
        return copy;
    }
}
