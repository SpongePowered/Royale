/*
 * This file is part of Royale, licensed under the MIT License (MIT).
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
package org.spongepowered.royale.instance;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;
import org.spongepowered.royale.instance.scoreboard.InstanceScoreboard;
import org.spongepowered.royale.instance.task.CleanupTask;
import org.spongepowered.royale.instance.task.EndTask;
import org.spongepowered.royale.instance.task.InstanceTask;
import org.spongepowered.royale.instance.task.ProgressTask;
import org.spongepowered.royale.instance.task.StartTask;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class Instance {

    private final Server server;
    private final InstanceManager instanceManager;
    private final ResourceKey worldKey;
    private final InstanceType instanceType;
    private final Set<UUID> registeredPlayers = new HashSet<>();
    private final Deque<Vector3d> unusedSpawns = Queues.newArrayDeque();
    private final Map<UUID, Vector3d> playerSpawns = new HashMap<>();
    private final Set<UUID> playerDeaths = new HashSet<>();
    private final Set<UUID> tasks = Sets.newLinkedHashSet();
    private final Map<Vector3i, BlockState> positionCache;
    private final InstanceScoreboard scoreboard;
    private State state = State.IDLE;

    public Instance(final Server server, final InstanceManager instanceManager, final ResourceKey worldKey, final InstanceType instanceType) {
        this.server = server;
        this.instanceManager = instanceManager;
        this.worldKey = worldKey;
        this.instanceType = instanceType;
        this.scoreboard = new InstanceScoreboard(this);
        this.positionCache = new HashMap<>();

        final ServerWorld world = this.server.getWorldManager().getWorld(worldKey)
                .orElseThrow(() -> new RuntimeException("Attempted to create an instance for an offline "
                        + "world!"));

        world.getBorder().setCenter(instanceType.getWorldBorderX(), instanceType.getWorldBorderZ());
        world.getBorder().setDiameter(instanceType.getWorldBorderRadius() * 2);
        world.getBorder().setWarningDistance(0);
    }

    public Server getServer() {
        return this.server;
    }

    public ResourceKey getWorldKey() {
        return this.worldKey;
    }

    public Optional<ServerWorld> getWorld() {
        return this.server.getWorldManager().getWorld(this.worldKey);
    }

    public InstanceType getType() {
        return this.instanceType;
    }

    public State getState() {
        return this.state;
    }

    public InstanceScoreboard getScoreboard() {
        return this.scoreboard;
    }

    public Map<Vector3i, BlockState> getPositionCache() {
        return this.positionCache;
    }

    public boolean isPlayerRegistered(final UUID uniqueId) {
        return this.registeredPlayers.contains(uniqueId);
    }

    public boolean isPlayerSpawned(final UUID uniqueId) {
        return this.playerSpawns.containsKey(uniqueId);
    }

    public boolean isPlayerDead(final UUID uniqueId) {
        return this.playerDeaths.contains(uniqueId);
    }

    public void advance() {
        this.server.getWorldManager().getWorld(worldKey)
                .orElseThrow(() -> new RuntimeException("Attempted to advance an instance for an offline "
                        + "world!"));

        final State next = State.values()[(this.state.ordinal() + 1) % State.values().length];
        if (next.ordinal() < this.state.ordinal()) {
            return;
        }

        this.advanceTo(next);
    }

    void advanceTo(State state) {
        final Optional<ServerWorld> world = this.server.getWorldManager().getWorld(this.worldKey);
        if (!world.isPresent()) {
            if (state == State.FORCE_STOP) {
                try {
                    this.instanceManager.unloadInstance(this.worldKey);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            throw new RuntimeException("Attempt to advance an instance whose world no longer exists!");
        }

        if (this.state == state) {
            return;
        }

        if (state.doesCheckRoundStatusOnAdvance()) {
            if (this.isRoundOver()) {
                state = State.PRE_END;
            }
        }

        this.onStateAdvance(state);
        this.state = state;
    }

    boolean canRegisterMorePlayers() {
        return this.unusedSpawns.size() != 0;
    }

    public boolean registerPlayer(final Player player) {
        if (this.unusedSpawns.isEmpty()) {
            player.sendMessage(Identity.nil(), Component.text("This instance cannot support any additional players!", NamedTextColor.RED));
            return false;
        }

        this.registeredPlayers.add(player.getUniqueId());
        return true;
    }

    public void addPlayerSpawn(final Vector3d spawn) {
        this.unusedSpawns.push(spawn);
    }

    public void spawnPlayer(final ServerPlayer player) {
        if (!this.registeredPlayers.contains(player.getUniqueId())) {
            throw new IllegalStateException("Attempted to spawn a player into this round who wasn't registered!");
        }

        final ServerWorld world = this.server.getWorldManager().getWorld(worldKey)
                .orElseThrow(() -> new RuntimeException("Attempted to spawn a player in an instance for an offline "
                        + "world!"));

        // If the player has a consumed spawn and this method is called, we put them back at spawn
        if (this.playerSpawns.containsKey(player.getUniqueId())) {
            player.setLocation(ServerLocation.of(world, this.playerSpawns.get(player.getUniqueId())));
            return;
        }

        checkState(!this.unusedSpawns.isEmpty(), "No spawn available for player!");
        final Vector3d player_spawn = this.unusedSpawns.pop();

        this.playerSpawns.put(player.getUniqueId(), player_spawn);

        this.scoreboard.addPlayer(player);

        player.setLocation(ServerLocation.of(world, player_spawn));

        final int playerCount = this.instanceType.getAutomaticStartPlayerCount();
        if (playerCount == this.registeredPlayers.size() && this.state == State.IDLE) {
            try {
                this.instanceManager.startInstance(this.worldKey);
            } catch (final UnknownInstanceException e) {
                e.printStackTrace();
            }
        }

        this.participate(player, true);
    }

    void disqualifyPlayer(final Player player) {
        this.scoreboard.killPlayer(player);
        this.playerDeaths.add(player.getUniqueId());
        player.getInventory().clear();
    }

    boolean isRoundOver() {
        return this.playerDeaths.size() >= this.playerSpawns.size() - 1;
    }

    private void participate(final ServerPlayer player, final boolean first) {
        Utils.resetPlayer(player);

        if (first) {
            player.getInventory().clear();

            for (final ItemStackSnapshot snapshot : this.instanceType.getDefaultItems()) {
                if (snapshot.getType() == ItemTypes.ELYTRA.get()) {
                    player.setChest(snapshot.createStack());
                } else {
                    player.getInventory().offer(snapshot.createStack());
                }
            }
        }
    }

    void spectate(final ServerPlayer player) {
        player.offer(Keys.GAME_MODE, GameModes.SPECTATOR.get());
    }

    private void onStateAdvance(final State next) {
        if (next.doesCancelTasksOnAdvance()) {
            // Cancel previous tasks when advancing a state. Prevents stale state tasks
            for (final UUID uuid : this.tasks) {
                final ScheduledTask task = Sponge.getServer().getScheduler().getTaskById(uuid).orElse(null);
                if (task != null) {
                    if (task.getTask().getConsumer() instanceof InstanceTask) {
                        ((InstanceTask) task.getTask().getConsumer()).cancel();
                    } else {
                        task.cancel();
                    }
                }
            }
        }

        switch (next) {
            case PRE_START:
                this.tasks.add(Sponge.getServer().getScheduler().submit(Task.builder()
                        .plugin(Royale.instance.getPlugin())
                        .execute(new StartTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Start Countdown - " + this.worldKey)
                        .build())
                        .getUniqueId());
                break;
            case POST_START:
                this.tasks.add(Sponge.getServer().getScheduler().submit(Task.builder()
                        .plugin(Royale.instance.getPlugin())
                        .execute(new ProgressTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Progress Countdown - " + this.worldKey)
                        .build())
                        .getUniqueId());
                break;
            case RUNNING:
                // TODO This activates before any additional code is called in the round task. Useless state for now, could be handy later
                break;
            case CLEANUP:
                this.tasks.add(Sponge.getServer().getScheduler().submit(Task.builder()
                        .plugin(Royale.instance.getPlugin())
                        .execute(new CleanupTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Cleanup - " + this.worldKey)
                        .build())
                        .getUniqueId());
                break;
            case PRE_END:
                final List<UUID> winners = new ArrayList<>(this.playerSpawns.keySet());
                winners.removeAll(this.playerDeaths);
                this.tasks.add(Sponge.getServer().getScheduler().submit(Task.builder()
                        .plugin(Royale.instance.getPlugin())
                        .execute(new EndTask(this, winners))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - End Countdown - " + this.worldKey)
                        .build())
                        .getUniqueId());
                break;
            case POST_END:
            case FORCE_STOP:
                try {
                    this.instanceManager.unloadInstance(this.worldKey);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.worldKey);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Instance instance = (Instance) o;
        return Objects.equals(this.worldKey, instance.worldKey);
    }

    @Override
    public String toString() {
        return com.google.common.base.MoreObjects.toStringHelper(this)
                .add("name", this.worldKey)
                .add("type", this.instanceType)
                .add("state", this.state)
                .toString();
    }

    interface InstanceState {

        default boolean canAnyoneMove() {
            return true;
        }

        default boolean canAnyoneInteract() {
            return false;
        }

        default boolean doesCancelTasksOnAdvance() {
            return true;
        }

        default boolean doesCheckRoundStatusOnAdvance() {
            return true;
        }
    }

    public enum State implements InstanceState {

        IDLE {
            @Override
            public boolean canAnyoneMove() {
                return false;
            }

            @Override
            public boolean doesCheckRoundStatusOnAdvance() {
                return false;
            }
        },
        PRE_START {
            @Override
            public boolean canAnyoneMove() {
                return false;
            }
        },
        POST_START {
            @Override
            public boolean canAnyoneInteract() {
                return true;
            }
        },
        RUNNING {
            @Override
            public boolean canAnyoneInteract() {
                return true;
            }

            @Override
            public boolean doesCancelTasksOnAdvance() {
                return false;
            }
        },
        CLEANUP {
            @Override
            public boolean canAnyoneInteract() {
                return true;
            }
        },
        PRE_END {
            @Override
            public boolean doesCheckRoundStatusOnAdvance() {
                return false;
            }
        },
        POST_END {
            @Override
            public boolean doesCheckRoundStatusOnAdvance() {
                return false;
            }
        },
        FORCE_STOP {
            @Override
            public boolean doesCheckRoundStatusOnAdvance() {
                return false;
            }
        }
    }
}
