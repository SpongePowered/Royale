package org.spongepowered.special.map;

import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.api.Server;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MapManager {

    final Map<String, Task> startTasks = new HashMap<>();
    final Map<String, Task> endTasks = new HashMap<>();

    public Optional<UUID> getStartTaskUniqueIdFor(World world) {
        checkNotNull(world);
        final Task task = this.startTasks.get(world.getName());
        if (task != null) {
            return Optional.of(task.getUniqueId());
        }
        return Optional.empty();
    }

    public Optional<UUID> getEndTaskUniqueIdFor(World world) {
        checkNotNull(world);
        final Task task = this.endTasks.get(world.getName());
        if (task != null) {
            return Optional.of(task.getUniqueId());
        }
        return Optional.empty();
    }

    /**
     * Prepares the {@link Server} to kick up the instance
     * @param mapType The map type to use
     */
    public void prepareInstance(MapType mapType) {

    }
}
