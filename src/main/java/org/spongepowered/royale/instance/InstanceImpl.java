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

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.api.Instance;
import org.spongepowered.royale.instance.scoreboard.InstanceScoreboard;
import org.spongepowered.royale.instance.task.OvertimeTask;
import org.spongepowered.royale.instance.task.EndTask;
import org.spongepowered.royale.instance.task.InstanceTask;
import org.spongepowered.royale.instance.task.ProgressTask;
import org.spongepowered.royale.instance.task.StartTask;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class InstanceImpl implements Instance {

    private final ResourceKey worldKey;
    private final InstanceType instanceType;
    private final Deque<Vector3d> unusedSpawns = new ArrayDeque<>();
    private final Map<UUID, Vector3d> playerSpawns = new HashMap<>();
    private final Set<UUID> playerDeaths = new HashSet<>();
    private final Set<UUID> tasks = new LinkedHashSet<>();
    private final InstanceScoreboard scoreboard;
    private final Set<ServerLocation> signLoc;
    private State state = State.IDLE;
    private UUID winner;
    private boolean unloading;

    public InstanceImpl(final ServerWorld world, final InstanceType instanceType) {
        this.worldKey = world.key();
        this.instanceType = instanceType;
        this.scoreboard = new InstanceScoreboard(this);
        this.signLoc = new HashSet<>();
    }

    @Override
    public ResourceKey getWorldKey() {
        return this.worldKey;
    }

    @Override
    public ServerWorld world() {
        return Sponge.server().worldManager().world(this.worldKey).orElseThrow(() -> new IllegalStateException("The world of this instance is unloaded"));
    }

    @Override
    public boolean addSpawnpoint(Vector3d spawn) {
        this.unusedSpawns.push(spawn);
        return true;
    }

    @Override
    public boolean addPlayer(ServerPlayer player) {
        if (this.isFull()) {
            throw new RuntimeException("Instance is full (" + this.unusedSpawns.size() + "/" + this.playerSpawns.size() + ")");
        }
        if (!this.state.canPlayersJoin()) {
            throw new IllegalStateException("This instance doesn't accept new players");
        }
        if (this.isPlayerRegistered(player)) {
            throw new IllegalArgumentException("Player is already registered");
        }

        final ServerWorld world = this.world();

        final Vector3d playerSpawn = this.unusedSpawns.pop();
        player.setLocation(ServerLocation.of(world, playerSpawn));
        this.playerSpawns.put(player.uniqueId(), playerSpawn);

        this.scoreboard.addPlayer(player);
        final int automaticStartPlayerCount = this.instanceType.getAutomaticStartPlayerCount();
        if (automaticStartPlayerCount != -1 && (this.isFull() || this.instanceType.getAutomaticStartPlayerCount() == this.playerSpawns.size())) {
            if (this.state == State.IDLE) {
                this.advance();
            }
        }

        this.resetPlayer(player);
        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL.get());
        this.updateSign();
        return true;
    }

    @Override
    public boolean removePlayer(final ServerPlayer player) {
        if (!this.isPlayerRegistered(player)) {
            throw new IllegalArgumentException("Player is not registered");
        }
        this.resetPlayer(player);
        player.offer(Keys.GAME_MODE, GameModes.SPECTATOR.get());

        if (this.state.canPlayersLeave()) {
            final Vector3d spawn = this.playerSpawns.remove(player.uniqueId());
            this.unusedSpawns.offer(spawn);
            this.scoreboard.removePlayer(player);
        } else {
            this.scoreboard.killPlayer(player);
            this.playerDeaths.add(player.uniqueId());

            final int playersLeft = this.playerSpawns.size() - this.playerDeaths.size();
            if (playersLeft > 0) {
                this.world().sendActionBar(Component.text(playersLeft + " players left", NamedTextColor.GREEN));
            }
            this.world().playSound(Sound.sound(SoundTypes.ENTITY_GHAST_HURT, Sound.Source.NEUTRAL, 0.5f, 0.7f));

            if (this.state != State.ENDING) {
                if (this.winner == null && this.playerDeaths.size() == this.playerSpawns.size() - 1) {
                    this.winner = this.playerSpawns.keySet().stream().filter(uuid -> !this.playerDeaths.contains(uuid)).limit(1).findAny().get();
                    this.advanceTo(State.ENDING);
                }
            }
        }
        this.updateSign();
        return true;
    }

    @Override
    public boolean addSpectator(ServerPlayer player) {
        if (this.isPlayerAlive(player)) {
            throw new IllegalArgumentException("Player is still alive!");
        }
        //TODO fix location
        player.setLocation(ServerLocation.of(this.worldKey, this.world().border().center()));
        player.offer(Keys.GAME_MODE, GameModes.SPECTATOR.get());
        //TODO night vision ?
        return true;
    }

    @Override
    public boolean isPlayerRegistered(ServerPlayer player) {
        return this.playerSpawns.containsKey(player.uniqueId());
    }

    @Override
    public boolean isPlayerAlive(ServerPlayer player) {
        return this.isPlayerRegistered(player) && !this.playerDeaths.contains(player.uniqueId());
    }

    @Override
    public boolean isFull() {
        return this.unusedSpawns.isEmpty();
    }

    @Override
    public Optional<UUID> getWinner() {
        return Optional.ofNullable(this.winner);
    }

    @Override
    public InstanceType getType() {
        return this.instanceType;
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public Collection<UUID> getPlayers() {
        return Collections.unmodifiableCollection(this.playerSpawns.keySet());
    }

    public void kickAll() {
        final ServerWorld lobby = Sponge.server().worldManager().world(Constants.Map.Lobby.LOBBY_WORLD_KEY)
                .orElse(Sponge.server().worldManager().defaultWorld());
        this.stopTasks();

        for (ServerPlayer player : this.world().players()) {
            player.sendMessage(Component.text("This instance is unloading. You are being moved to the lobby"));
            this.scoreboard.removePlayer(player);
            Sponge.server().serverScoreboard().ifPresent(player::setScoreboard);
            player.setLocation(ServerLocation.of(lobby, lobby.properties().spawnPosition()));
            this.resetPlayer(player);
            player.offer(Keys.GAME_MODE, GameModes.SURVIVAL.get());
        }
    }

    private void resetPlayer(final ServerPlayer player) {
        player.offer(Keys.HEALTH, 20d);
        player.offer(Keys.FOOD_LEVEL, 20);
        player.offer(Keys.SATURATION, 20d);
        player.offer(Keys.EXHAUSTION, 20d);
        player.remove(Keys.POTION_EFFECTS);
        player.inventory().clear();
    }

    public int playersLeft() {
        return this.playerSpawns.size() - this.playerDeaths.size();
    }

    private int spawns() {
        return this.playerSpawns.size() + this.unusedSpawns.size();
    }

    public void advance() {
        if (this.state.ordinal() == State.values().length - 1) {
            throw new IllegalStateException("Can't advance");
        }
        State next = State.values()[this.state.ordinal() + 1];
        this.advanceTo(next);
    }

    void advanceTo(State state) {
        Royale.getInstance().getPlugin().getLogger().debug("Advancing {} from {} to {}", this.worldKey.formatted(), this.state.name(), state.name());
        if (this.state == state) {
            throw new IllegalArgumentException("The instance is already at " + state.name());
        }
        if (this.state == State.STOPPED) {
            throw new IllegalStateException("The instance is stopped");
        }

        this.onStateAdvance(state);
        this.state = state;
    }

    private void onStateAdvance(final State next) {
        this.stopTasks();
        switch (next) {
            case STARTING:
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.getInstance().getPlugin())
                        .execute(new StartTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Start Countdown - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case RUNNING:
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.getInstance().getPlugin())
                        .execute(new ProgressTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Progress Countdown - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case OVERTIME:
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.getInstance().getPlugin())
                        .execute(new OvertimeTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Overtime - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case ENDING:
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.getInstance().getPlugin())
                        .execute(new EndTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - End Countdown - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case STOPPED:
                this.unloading = true;
                Royale.getInstance().getInstanceManager().unloadInstance(this.worldKey)
                        .thenComposeAsync(b -> Royale.getInstance().getInstanceManager().createInstance(this.worldKey, this.instanceType, false), Royale.getInstance().getTaskExecutorService())
                        .thenAcceptAsync(instance -> {
                            for (ServerLocation location : this.signLoc) {
                                location.blockEntity()
                                        .filter(blockEntity -> blockEntity instanceof Sign)
                                        .ifPresent(sign -> instance.link((Sign) sign));
                            }
                        }, Royale.getInstance().getTaskExecutorService());
                break;
        }
        this.updateSign();
    }

    private void stopTasks() {
        this.tasks.removeIf(uuid -> {
            final Optional<ScheduledTask> taskOpt = Sponge.server().scheduler().taskById(uuid);
            if (!taskOpt.isPresent()) {
                Royale.getInstance().getPlugin().getLogger().warn("Missing task with UUID {} while in state {}", uuid, this.state);
                return true;
            }

            if (taskOpt.get().isCancelled()) {
                Royale.getInstance().getPlugin().getLogger().warn("Cancelled task with UUID {} while in state {}", uuid, this.state);
                return true;
            }

            if (!(taskOpt.get().task().consumer() instanceof InstanceTask)) {
                Royale.getInstance().getPlugin().getLogger().warn("Malformed task with UUID {} while in state {}", uuid, this.state);
                return true;
            }

            ((InstanceTask) taskOpt.get().task().consumer()).cleanup();
            taskOpt.get().cancel();
            return true;
        });
    }

    @Override
    public int hashCode() {
        return this.worldKey.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final InstanceImpl instance = (InstanceImpl) o;
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

    public void setUnloading(boolean unloading) {
        this.unloading = unloading;
    }

    @Override
    public boolean link(Sign sign) {
        if (this.signLoc.add(sign.serverLocation())) {
            this.updateSign0(sign);
            return true;
        }
        return false;
    }

    void updateSign() {
        for (ServerLocation location : this.signLoc) {
            location.worldIfAvailable().flatMap(w -> location.blockEntity()).ifPresent(this::updateSign0);
        }
    }

    private void updateSign0(org.spongepowered.api.block.entity.BlockEntity sign) {
        Component statusLine;
        Component headerLine;
        final int playersTotal = this.playerSpawns.size();
        switch (this.state) {
            case ENDING:
                headerLine = Component.text("Create Game", NamedTextColor.AQUA);
                statusLine = Component.text("not ready", NamedTextColor.YELLOW);
                break;
            case IDLE:
                headerLine = Component.text("Join Game", NamedTextColor.AQUA);
                statusLine = Component.text("waiting " + playersTotal + "/" + this.spawns(), NamedTextColor.GREEN);
                break;
            case RUNNING:
                headerLine = Component.text("Spectate Game", NamedTextColor.AQUA);
                statusLine = Component.text("running " + this.playersLeft() + "/" + playersTotal, NamedTextColor.YELLOW);
                break;
            case OVERTIME:
                headerLine = Component.text("Spectate Game", NamedTextColor.AQUA);
                statusLine = Component.text("overtime " + (playersTotal - this.playersLeft()) + "/" + playersTotal, NamedTextColor.YELLOW);
                break;
            default:
                headerLine = Component.text("Royale", NamedTextColor.AQUA);
                statusLine = Component.text(this.state.name());
        }
        sign.transform(Keys.SIGN_LINES, lines -> {
            lines.set(0, headerLine);
            lines.set(1, Component.text(this.worldKey.asString()));
            lines.set(2, statusLine);
            lines.set(3, Component.text(this.instanceType.name()));
            return lines;
        });
    }
}
