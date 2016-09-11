/**
 * This file is part of Special, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <http://github.com/SpongePowered>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.special.instance;

import static com.google.common.base.Preconditions.checkState;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class Instance {

    private final String worldName;
    private final InstanceType instanceType;
    private final WeakReference<World> worldRef;
    private final Deque<Vector3d> unusedSpawns = Queues.newArrayDeque();
    private final Map<UUID, Vector3d> playerSpawns = Maps.newHashMap();
    private final Set<UUID> tasks = Sets.newHashSet();

    private State state = State.IDLE;

    public Instance(String worldName, InstanceType instanceType, World world) {
        this.worldName = worldName;
        this.instanceType = instanceType;
        this.worldRef = new WeakReference<>(world);
    }

    public void start() {

        final World world = this.worldRef.get();

        if (world == null) {
            throw new RuntimeException("Attempt to start an instance whose world no longer exists!");
        }

        this.advance();
    }

    public void stop() {
        for (UUID uuid : tasks) {
            final Task task = Sponge.getScheduler().getTaskById(uuid).orElse(null);
            if (task != null) {
                task.cancel();
            }
        }

        this.advance();
    }

    public InstanceType getType() {
        return this.instanceType;
    }

    public Optional<World> getHandle() {
        return Optional.ofNullable(this.worldRef.get());
    }

    public Set<UUID> getAssociatedTasks() {
        return Collections.unmodifiableSet(this.tasks);
    }

    public boolean addAssociatedTaskUniqueId(UUID uniqueId) {
        return this.tasks.add(uniqueId);
    }

    public boolean removeAssociatedTaskUniqueId(UUID uniqueId) {
        return this.tasks.remove(uniqueId);
    }

    public State getState() {
        return state;
    }

    public void advance() {
        final State next = State.values()[(this.state.ordinal() + 1) % State.values().length];
        if (next.ordinal() < this.state.ordinal()) {
            return;
        }

        this.onStateAdvance(next);
        this.state = next;
    }

    public void onStateAdvance(State next) {
        switch (next) {
            case PRE_START:
                // TODO Start initial task
                break;
            case POST_START:
                // TODO Start round task (kill start task if still running)
                break;
            case RUNNING:
                // TODO This activates before any additional code is called in the round task. Useless state for now, could be handy later
                break;
            case PRE_END:
                // TODO Start end task (terminate round task if still running)
                break;
            case POST_END:
                break;
        }
    }

    public void addPlayerSpawn(Vector3d spawn) {
        this.unusedSpawns.push(spawn);
    }

    public void spawnPlayer(Player player) {
        checkState(this.state != State.IDLE, "Instance is not running!");
        if (this.playerSpawns.containsKey(player.getUniqueId())) {
            player.setLocation(new Location<>(this.worldRef.get(), this.playerSpawns.get(player.getUniqueId())));
            return;
        }
        checkState(!this.unusedSpawns.isEmpty(), "No spawn available for player!");
        Vector3d player_spawn = this.unusedSpawns.pop();
        this.playerSpawns.put(player.getUniqueId(), player_spawn);

        player.setLocation(new Location<>(this.worldRef.get(), player_spawn));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Instance instance = (Instance) o;
        return Objects.equals(this.worldName, instance.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.worldName);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("name", this.worldName)
                .add("type", this.instanceType)
                .toString();
    }

    public enum State {
        /**
         * Instance created but not activated.
         */
        IDLE,
        /**
         * Instance activated but not started.
         */
        PRE_START,
        POST_START,
        RUNNING,
        PRE_END,
        POST_END
    }
}
