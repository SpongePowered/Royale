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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.entity.Sign;
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
import org.spongepowered.royale.api.Instance;
import org.spongepowered.royale.api.InstanceState;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;
import org.spongepowered.royale.instance.scoreboard.InstanceScoreboard;
import org.spongepowered.royale.instance.task.CleanupTask;
import org.spongepowered.royale.instance.task.EndTask;
import org.spongepowered.royale.instance.task.InstanceTask;
import org.spongepowered.royale.instance.task.ProgressTask;
import org.spongepowered.royale.instance.task.StartTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
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

public final class InstanceImpl implements Instance {

    private final ResourceKey worldKey;
    private final InstanceType instanceType;
    private final Set<UUID> registeredPlayers = new HashSet<>();
    private final Deque<Vector3d> unusedSpawns = new ArrayDeque<>();
    private final Map<UUID, Vector3d> playerSpawns = new HashMap<>();
    private final Set<UUID> playerDeaths = new HashSet<>();
    private final Set<UUID> tasks = new LinkedHashSet<>();
    private final InstanceScoreboard scoreboard;
    private final List<ServerLocation> signLoc;
    private State state = State.IDLE;

    public InstanceImpl(final ServerWorld world, final InstanceType instanceType) {
        this.worldKey = world.key();
        this.instanceType = instanceType;
        this.scoreboard = new InstanceScoreboard(this);
        this.signLoc = new ArrayList<>();
    }

    @Override
    public ResourceKey getWorldKey() {
        return this.worldKey;
    }

    public Optional<ServerWorld> getWorld() {
        return Sponge.server().worldManager().world(this.worldKey);
    }

    @Override
    public boolean addSpawnpoint(Vector3d spawn) {
        this.unusedSpawns.push(spawn);
        return true;
    }

    @Override
    public boolean addPlayer(ServerPlayer player) {
        if (this.isFull()) {
            throw new RuntimeException("Instance is full (" + this.unusedSpawns.size() + "/" + this.registeredPlayers.size() + ")");
        }
        return this.registeredPlayers.add(player.uniqueId());
    }

    @Override
    public boolean isFull() {
        return this.unusedSpawns.isEmpty();
    }

    @Override
    public InstanceType getType() {
        return this.instanceType;
    }

    @Override
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
        final ServerWorld instanceWorld = Sponge.server().worldManager().world(this.worldKey).orElse(null);
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
        final Optional<ServerWorld> world = Sponge.server().worldManager().world(this.worldKey);
        if (!world.isPresent()) {
            this.state = State.FORCE_STOP;
            Royale.getInstance().getInstanceManager().unloadInstance(this.worldKey).join();
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

    public boolean registerPlayer(final Player player) {
        if (this.unusedSpawns.isEmpty()) {
            player.sendMessage(Identity.nil(), Component.text("This instance cannot support any additional players!", NamedTextColor.RED));
            return false;
        }

        this.registeredPlayers.add(player.uniqueId());
        return true;
    }

    public void spawnPlayer(final ServerPlayer player) {
        if (!this.registeredPlayers.contains(player.uniqueId())) {
            throw new IllegalStateException("Attempted to spawn a player into this round who wasn't registered!");
        }

        final ServerWorld world = Sponge.server().worldManager().world(this.worldKey)
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
                Royale.getInstance().getInstanceManager().startInstance(this.worldKey);
            } catch (final UnknownInstanceException e) {
                e.printStackTrace();
            }
        }

        this.participate(player, true);

        player.sendMessage(Component.text("Welcome to the game. Please stand by while others join. You will not be able to move until the game "
                + "starts."));
    }

    @Override
    public boolean disqualifyPlayers(final Collection<ServerPlayer> players) {
        if (this.state == State.IDLE) {
            for (ServerPlayer player : players) {
                this.registeredPlayers.remove(player.uniqueId());
                final Vector3d spawn = this.playerSpawns.remove(player.uniqueId());
                this.unusedSpawns.offer(spawn);

                this.scoreboard.removePlayer(player);
            }
        } else {
            for (ServerPlayer player : players) {
                this.removePlayer(player);
                this.resetPlayer(player);
                player.offer(Keys.GAME_MODE, GameModes.SPECTATOR.get());
            }
            final int playersLeft = this.registeredPlayers.size() - this.playerDeaths.size();
            if (playersLeft > 0) {
                getWorld().get().sendActionBar(Component.text(playersLeft + " players left", NamedTextColor.GREEN));
            }
            getWorld().get().playSound(Sound.sound(SoundTypes.ENTITY_GHAST_HURT, Sound.Source.NEUTRAL, 0.5f, 0.7f));
        }
        this.updateSign();
        return true;
    }

    @Override
    public boolean kick(Collection<ServerPlayer> players) {
        disqualifyPlayers(players);
        for (ServerPlayer player : players) {
            final Optional<ServerWorld> lobby = Sponge.server().worldManager().world(Constants.Map.Lobby.LOBBY_WORLD_KEY);
            if (!lobby.isPresent()) {
                throw new IllegalStateException("Lobby world was not found!");
            }
            Sponge.server().serverScoreboard().ifPresent(player::setScoreboard);
            player.setLocation(ServerLocation.of(lobby.get(), lobby.get().properties().spawnPosition()));
            player.offer(Keys.GAME_MODE, GameModes.SURVIVAL.get());
        }
        return true;
    }

    private void removePlayer(final ServerPlayer player) {
        this.scoreboard.killPlayer(player);
        this.playerDeaths.add(player.uniqueId());
        player.inventory().clear();
    }

    private void resetPlayer(final ServerPlayer player) {
        player.offer(Keys.HEALTH, 20d);
        player.offer(Keys.FOOD_LEVEL, 20);
        player.offer(Keys.SATURATION, 20d);
        player.offer(Keys.EXHAUSTION, 20d);
        player.remove(Keys.POTION_EFFECTS);
    }

    public int playersLeft() {
        return this.playerSpawns.size() - this.playerDeaths.size();
    }

    boolean isRoundOver() {
        return this.playerDeaths.size() >= this.playerSpawns.size() - 1;
    }

    private void participate(final ServerPlayer player, final boolean first) {
        this.resetPlayer(player);
        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL.get());

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
                        .plugin(Royale.getInstance().getPlugin())
                        .execute(new StartTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - Start Countdown - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case POST_START:
                this.tasks.add(Sponge.server().scheduler().submit(Task.builder()
                        .plugin(Royale.getInstance().getPlugin())
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
                        .plugin(Royale.getInstance().getPlugin())
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
                        .plugin(Royale.getInstance().getPlugin())
                        .execute(new EndTask(this, winners))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Plugin.ID + " - End Countdown - " + this.worldKey)
                        .build()
                ).uniqueId());
                break;
            case POST_END:
            case FORCE_STOP:
                Royale.getInstance().getInstanceManager().unloadInstance(this.worldKey).join();
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

    public int spawns() {
        return this.registeredPlayers.size() + this.unusedSpawns.size();
    }

    @Override
    public boolean link(Sign sign) {
        if (this.signLoc.add(sign.serverLocation())) {
            updateSign0(sign);
            return true;
        }
        return false;
    }

    public void updateSign() {
        for (ServerLocation location : this.signLoc) {
            if (location.world().isLoaded()) {
                location.blockEntity().ifPresent(this::updateSign0);
            }
        }
    }

    private void updateSign0(org.spongepowered.api.block.entity.BlockEntity sign) {
        Component statusLine;
        Component headerLine;
        final int playersTotal = this.registeredPlayers();
        switch (this.state) {
            case PRE_END:
                headerLine = Component.text("Create Game", NamedTextColor.AQUA);
                statusLine = Component.text("not ready", NamedTextColor.YELLOW);
                break;
            case IDLE:
                headerLine = Component.text("Join Game", NamedTextColor.AQUA);
                statusLine = Component.text("waiting " + playersTotal + "/" + this.spawns(), NamedTextColor.GREEN);
                break;
            case RUNNING:
                headerLine = Component.text("Spectate Game", NamedTextColor.AQUA);
                statusLine = Component.text("running " + playersLeft() + "/" + playersTotal, NamedTextColor.YELLOW);
                break;
            case CLEANUP:
                headerLine = Component.text("Spectate Game", NamedTextColor.AQUA);
                statusLine = Component.text("cleanup " + (playersTotal - playersLeft()) + "/" + playersTotal, NamedTextColor.YELLOW);
                break;
            default:
                headerLine = Component.text("Royale", NamedTextColor.AQUA);
                statusLine = Component.text(this.state.name());
        }
        sign.transform(Keys.SIGN_LINES, lines -> {
            lines.set(0, headerLine);
            lines.set(1, this.getWorld().flatMap(w -> w.properties().displayName()).orElse(Component.text(this.worldKey.asString())));
            lines.set(2, statusLine);
            lines.set(3, Component.text(this.instanceType.name()));
            return lines;
        });
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
