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
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Constants;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.exception.UnknownInstanceException;
import org.spongepowered.special.instance.scoreboard.RoundScoreboard;
import org.spongepowered.special.instance.task.CleanupTask;
import org.spongepowered.special.instance.task.EndTask;
import org.spongepowered.special.instance.task.InstanceTask;
import org.spongepowered.special.instance.task.ProgressTask;
import org.spongepowered.special.instance.task.StartTask;

import java.lang.ref.WeakReference;
import java.time.Instant;
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

    private final String worldName;
    private final InstanceType instanceType;
    private final WeakReference<World> worldRef;
    private final Set<UUID> registeredPlayers = new HashSet<>();
    private final Deque<Vector3d> unusedSpawns = Queues.newArrayDeque();
    private final Map<UUID, Vector3d> playerSpawns = new HashMap<>();
    private final Map<UUID, PlayerDeathRecord> playerDeaths = new HashMap<>();
    private final Set<UUID> tasks = Sets.newLinkedHashSet();
    private final RoundScoreboard scoreboard;
    private State state = State.IDLE;

    public Instance(String worldName, InstanceType instanceType, World world) {
        this.worldName = worldName;
        this.instanceType = instanceType;
        this.worldRef = new WeakReference<>(world);
        this.scoreboard = new RoundScoreboard(this);
        world.getWorldBorder().setCenter(instanceType.getWorldBorderX(), instanceType.getWorldBorderZ());
        world.getWorldBorder().setDiameter(instanceType.getWorldBorderRadius() * 2);
        world.getWorldBorder().setWarningDistance(0);
    }

    public String getName() {
        return worldName;
    }

    public InstanceType getType() {
        return this.instanceType;
    }

    public Optional<World> getHandle() {
        return Optional.ofNullable(this.worldRef.get());
    }

    public State getState() {
        return state;
    }

    RoundScoreboard getScoreboard() {
        return scoreboard;
    }

    public Set<UUID> getRegisteredPlayers() {
        return registeredPlayers;
    }

    Map<UUID, Vector3d> getPlayerSpawns() {
        return playerSpawns;
    }

    Map<UUID, PlayerDeathRecord> getPlayerDeaths() {
        return playerDeaths;
    }

    public void advance() {
        if (this.worldRef.get() == null) {
            throw new RuntimeException("Attempt to advance an instance whose world no longer exists!");
        }

        final State next = State.values()[(this.state.ordinal() + 1) % State.values().length];
        if (next.ordinal() < this.state.ordinal()) {
            return;
        }

        this.advanceTo(next);
    }

    void advanceTo(State state) {
        if (this.worldRef.get() == null) {
            if (state == State.FORCE_STOP) {
                Special.instance.getInstanceManager().unloadInstance(this);
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

    public void registerPlayer(Player player) {
        checkState(this.unusedSpawns.size() != 0, "This instance cannot register any more players!");
        this.registeredPlayers.add(player.getUniqueId());
    }

    public void addPlayerSpawn(Vector3d spawn) {
        this.unusedSpawns.push(spawn);
    }

    public void spawnPlayer(Player player) {
        checkState(this.registeredPlayers.contains(player.getUniqueId()), "Attempted to spawn a player into this round who wasn't registered!");

        final World world = this.worldRef.get();
        checkState(world != null, "Instance world ref is null!");

        // If the player has a consumed spawn and this method is called, we put them back at spawn
        if (this.playerSpawns.containsKey(player.getUniqueId())) {
            player.setLocation(new Location<>(world, this.playerSpawns.get(player.getUniqueId())));
            return;
        }

        checkState(!this.unusedSpawns.isEmpty(), "No spawn available for player!");
        Vector3d player_spawn = this.unusedSpawns.pop();

        this.playerSpawns.put(player.getUniqueId(), player_spawn);

        this.scoreboard.addPlayer(player);

        player.setLocation(new Location<>(world, player_spawn));

        int playerCount = this.instanceType.getAutomaticStartPlayerCount();
        if (playerCount == this.registeredPlayers.size() && this.state == State.IDLE) {
            try {
                Special.instance.getInstanceManager().startInstance(this.worldName);
            } catch (UnknownInstanceException e) {
                e.printStackTrace();
            }
        }

        this.convertPlayerToCombatant(player, true);
    }

    void disqualifyPlayer(Player player, Cause causeOfDeath) {
        this.playerDeaths.put(player.getUniqueId(), new PlayerDeathRecord(player, causeOfDeath));
        this.scoreboard.killPlayer(player);
        player.getInventory().clear();
    }

    boolean isRoundOver() {
        return this.playerDeaths.size() >= this.playerSpawns.size() - 1;

    }

    private void convertPlayerToCombatant(Player player, boolean first) {
        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
        Utils.resetHealthHungerAndPotions(player);

        if (first) {
            player.getInventory().clear();

            for (ItemStackSnapshot snapshot : this.instanceType.getDefaultItems()) {
                if (snapshot.getType() == ItemTypes.ELYTRA) {
                    ((EntityPlayerMP) player).setItemStackToSlot(EntityEquipmentSlot.CHEST, (ItemStack) (Object) snapshot.createStack());
                } else {
                    player.getInventory().offer(snapshot.createStack());
                }
            }
        }
    }

    void convertPlayerToSpectator(Player player) {
        player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
    }

    private void onStateAdvance(State next) {
        if (next.doesCancelTasksOnAdvance()) {
            // Cancel previous tasks when advancing a state. Prevents stale state tasks
            for (UUID uuid : this.tasks) {
                final Task task = Sponge.getScheduler().getTaskById(uuid).orElse(null);
                if (task != null) {
                    if (task.getConsumer() instanceof InstanceTask) {
                        ((InstanceTask) task.getConsumer()).cancel();
                    } else {
                        task.cancel();
                    }
                }
            }
        }

        switch (next) {
            case PRE_START:
                this.tasks.add(Task.builder()
                        .execute(new StartTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Meta.ID + " - Start Countdown - " + this.worldName)
                        .submit(Special.instance)
                        .getUniqueId());
                break;
            case POST_START:
                this.tasks.add(Task.builder()
                        .execute(new ProgressTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Meta.ID + " - Progress Countdown - " + this.worldName)
                        .submit(Special.instance)
                        .getUniqueId());
                break;
            case RUNNING:
                // TODO This activates before any additional code is called in the round task. Useless state for now, could be handy later
                break;
            case CLEANUP:
                this.tasks.add(Task.builder()
                        .execute(new CleanupTask(this))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Meta.ID + " - Cleanup - " + this.worldName)
                        .submit(Special.instance)
                        .getUniqueId());
                break;
            case PRE_END:
                final List<UUID> winners = new ArrayList<>();
                winners.addAll(this.playerSpawns.keySet());
                winners.removeAll(this.playerDeaths.keySet());
                this.tasks.add(Task.builder()
                        .execute(new EndTask(this, winners))
                        .interval(1, TimeUnit.SECONDS)
                        .name(Constants.Meta.ID + " - End Countdown - " + this.worldName)
                        .submit(Special.instance)
                        .getUniqueId());
                break;
            case POST_END:
            case FORCE_STOP:
                Special.instance.getInstanceManager().unloadInstance(this);
                break;
        }
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
                .add("state", this.state)
                .toString();
    }

    public enum State implements IState {

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

    interface IState {

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

    /**
     * TODO Put this to use in a future feature.
     */
    private static class PlayerDeathRecord {

        private final UUID victim;
        private final Cause causeOfDeath;
        private final Instant timeOfDeath;

        PlayerDeathRecord(Player victim, Cause causeOfDeath) {
            this(victim, causeOfDeath, Instant.now());
        }

        PlayerDeathRecord(Player victim, Cause causeOfDeath, Instant timeOfDeath) {
            this.victim = victim.getUniqueId();
            this.causeOfDeath = causeOfDeath;
            this.timeOfDeath = timeOfDeath;
        }
    }
}
