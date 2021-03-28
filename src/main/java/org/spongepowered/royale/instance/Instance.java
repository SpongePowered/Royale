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

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;
import org.spongepowered.royale.instance.scoreboard.InstanceScoreboard;
import org.spongepowered.royale.instance.task.CleanupTask;
import org.spongepowered.royale.instance.task.EndTask;
import org.spongepowered.royale.instance.task.InstanceTask;
import org.spongepowered.royale.instance.task.ProgressTask;
import org.spongepowered.royale.instance.task.StartTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private final Deque<Vector3d> unusedSpawns = new ArrayDeque<>();
    private final Map<UUID, Vector3d> playerSpawns = new HashMap<>();
    private final Set<UUID> playerDeaths = new HashSet<>();
    private final Set<UUID> tasks = new LinkedHashSet<>();
    private final InstanceScoreboard scoreboard;
    private State state = State.IDLE;
    private ServerLocation signLoc;

    public Instance(final Server server, final InstanceManager instanceManager, final ResourceKey worldKey, final InstanceType instanceType) {
        this.server = server;
        this.instanceManager = instanceManager;
        this.worldKey = worldKey;
        this.instanceType = instanceType;
        this.scoreboard = new InstanceScoreboard(this);

        final ServerWorld world = this.server.worldManager().world(worldKey)
                .orElseThrow(() -> new RuntimeException("Attempted to create an instance for an offline "
                        + "world!"));

        world.border().setCenter(instanceType.getWorldBorderX(), instanceType.getWorldBorderZ());
        world.border().setDiameter(instanceType.getWorldBorderRadius() * 2);
        world.border().setWarningDistance(0);
    }

    public Server getServer() {
        return this.server;
    }

    public ResourceKey getWorldKey() {
        return this.worldKey;
    }

    public Optional<ServerWorld> getWorld() {
        return this.server.worldManager().world(this.worldKey);
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

    public boolean isPlayerRegistered(final UUID uniqueId) {
        return this.registeredPlayers.contains(uniqueId);
    }

    public int registeredPlayers() {
        return this.registeredPlayers.size();
    }

    public boolean isPlayerSpawned(final UUID uniqueId) {
        return this.playerSpawns.containsKey(uniqueId);
    }

    public boolean isPlayerDead(final UUID uniqueId) {
        return this.playerDeaths.contains(uniqueId);
    }

    public void advance() {
        final ServerWorld instanceWorld = this.server.worldManager().world(this.worldKey).orElse(null);
        State next;
        if (instanceWorld == null) {
            next = State.FORCE_STOP;
        } else {
            next = State.values()[(this.state.ordinal() + 1) % State.values().length];
            if (next.ordinal() < this.state.ordinal()) {
                return;
            }
        }

        this.advanceTo(next);
    }

    void advanceTo(State state) {
        final Optional<ServerWorld> world = this.server.worldManager().world(this.worldKey);
        if (!world.isPresent()) {
            this.state = State.FORCE_STOP;
            try {
                this.instanceManager.unloadInstance(this.worldKey);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            return;
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

        this.registeredPlayers.add(player.uniqueId());
        return true;
    }

    public void addPlayerSpawn(final Vector3d spawn) {
        this.unusedSpawns.push(spawn);
    }

    public void spawnPlayer(final ServerPlayer player) {
        if (!this.registeredPlayers.contains(player.uniqueId())) {
            throw new IllegalStateException("Attempted to spawn a player into this round who wasn't registered!");
        }

        final ServerWorld world = this.server.worldManager().world(this.worldKey)
                .orElseThrow(() -> new RuntimeException("Attempted to spawn a player in an instance for an offline "
                        + "world!"));

        // If the player has a consumed spawn and this method is called, we put them back at spawn
        if (this.playerSpawns.containsKey(player.uniqueId())) {
            player.setLocation(ServerLocation.of(world, this.playerSpawns.get(player.uniqueId())));
            return;
        }

        if (this.unusedSpawns.isEmpty()) {
            throw new IllegalStateException("No spawn available for player!");
        }

        final Vector3d playerSpawn = this.unusedSpawns.pop();

        this.scoreboard.addPlayer(player);

        player.setLocation(ServerLocation.of(world, playerSpawn));

        this.playerSpawns.put(player.uniqueId(), playerSpawn);

        final int playerCount = this.instanceType.getAutomaticStartPlayerCount();
        if (playerCount == this.registeredPlayers.size() && this.state == State.IDLE) {
            try {
                this.instanceManager.startInstance(this.worldKey);
            } catch (final UnknownInstanceException e) {
                e.printStackTrace();
            }
        }

        this.participate(player, true);

        player.sendMessage(Component.text("Welcome to the game. Please stand by while others join. You will not be able to move until the game "
                + "starts."));
    }

    void disqualifyPlayer(final Player player) {
        this.scoreboard.killPlayer(player);
        this.playerDeaths.add(player.uniqueId());
        player.inventory().clear();
        player.offer(Keys.GAME_MODE, GameModes.SPECTATOR.get());
        final int playersLeft = this.playersLeft();
        if (playersLeft > 0) {
            for (Player playerInWorld : player.world().players()) {
                playerInWorld.sendActionBar(Component.text(playersLeft + " players left", NamedTextColor.GREEN));
            }
        }
        player.world().playSound(Sound.sound(SoundTypes.ENTITY_GHAST_HURT, Sound.Source.NEUTRAL, 0.5f, 0.7f));
        this.updateSign();
    }

    public int playersLeft() {
        return this.playerSpawns.size() - this.playerDeaths.size();
    }

    boolean isRoundOver() {
        return this.playerDeaths.size() >= this.playerSpawns.size() - 1;
    }

    private void participate(final ServerPlayer player, final boolean first) {
        Utils.resetPlayer(player);

        if (first) {
            player.inventory().clear();

            for (final ItemStackSnapshot snapshot : this.instanceType.getDefaultItems()) {
                if (snapshot.type() == ItemTypes.ELYTRA.get()) {
                    player.setChest(snapshot.createStack());
                } else {
                    player.inventory().offer(snapshot.createStack());
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
                final ScheduledTask task = Sponge.server().scheduler().taskById(uuid).orElse(null);
                if (task != null) {
                    if (task.task().consumer() instanceof InstanceTask) {
                        ((InstanceTask) task.task().consumer()).cancel();
                    } else {
                        task.cancel();
                    }
                }
            }

            this.tasks.clear();
        }

        switch (next) {
            case PRE_START:
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.instance.getPlugin())
                        .execute(new StartTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Start Countdown - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case POST_START:
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.instance.getPlugin())
                        .execute(new ProgressTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Progress Countdown - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case RUNNING:
                break;
            case CLEANUP:
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.instance.getPlugin())
                        .execute(new CleanupTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Cleanup - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case PRE_END:
                final List<UUID> winners = new ArrayList<>(this.playerSpawns.keySet());
                winners.removeAll(this.playerDeaths);
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.instance.getPlugin())
                        .execute(new EndTask(this, winners))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - End Countdown - " + this.worldKey)
                        .build()
                ).uniqueId());
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
        this.updateSign();
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

    public int spawns() {
        return this.registeredPlayers.size() + this.unusedSpawns.size();
    }

    public void assign(ServerLocation signLoc) {
        this.signLoc = signLoc;
    }

    public void updateSign() {
        if (this.signLoc.world().isLoaded()) {
            this.signLoc.blockEntity().ifPresent(this::updateSign0);
        }
    }

    private void updateSign0(org.spongepowered.api.block.entity.BlockEntity sign) {
        Component statusLine;
        Component headerLine = Component.text("Join Game", NamedTextColor.AQUA);
        final int playersTotal = this.registeredPlayers();
        switch (this.state) {
            case PRE_END:
                statusLine = Component.text("not ready", NamedTextColor.YELLOW);
                break;
            case IDLE:
                statusLine = Component.text("waiting " + playersTotal + "/" + this.spawns(), NamedTextColor.GREEN);
                break;
            case RUNNING:
                statusLine = Component.text("running " + playersLeft() + "/" + playersTotal, NamedTextColor.YELLOW);
                break;
            case CLEANUP:
                statusLine = Component.text("cleanup " + (playersTotal - playersLeft()) + "/" + playersTotal, NamedTextColor.YELLOW);
                break;
            default:
                statusLine = Component.text(this.state.name());
        }
        sign.transform(Keys.SIGN_LINES, lines -> {
            lines.set(0, headerLine);
            lines.set(2, statusLine);
            return lines;
        });
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
