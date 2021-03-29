package org.spongepowered.royale.api;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.royale.instance.InstanceType;

public interface Instance {

    boolean addSpawnpoint(Vector3d vector3d);

    boolean isFull();

    InstanceType getType();

    ResourceKey getWorldKey();

    boolean link(Sign sign);

}
