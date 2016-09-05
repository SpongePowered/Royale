package org.spongepowered.special.task;

import org.spongepowered.api.world.World;
import org.spongepowered.special.map.MapType;

import java.lang.ref.WeakReference;

public abstract class RoundCountdown implements Runnable {

    private final MapType mapType;
    private final WeakReference<World> worldRef;

    public RoundCountdown(MapType mapType, World world) {
        this.mapType = mapType;
        this.worldRef = new WeakReference<>(world);
    }

    public final MapType getMapType() {
        return this.mapType;
    }

    public final WeakReference<World> getWorldRef() {
        return this.worldRef;
    }
}