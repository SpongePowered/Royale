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

import com.google.common.collect.Iterables;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.entity.ChangeSignEvent;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.instance.exception.InstanceAlreadyExistsException;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;
import org.spongepowered.royale.instance.gen.InstanceMutatorPipeline;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public final class InstanceManager {

    private final Game game;
    private final Map<ResourceKey, Instance> instances;
    private final Map<InstanceType, List<Instance>> instancesByTypes;
    private final Set<ResourceKey> canUseFastPass;
    private final Set<UUID> forceRespawning;

    public InstanceManager(final Game game) {
        this.game = game;
        this.instances = new HashMap<>();
        this.instancesByTypes = new HashMap<>();
        this.canUseFastPass = new HashSet<>();
        this.forceRespawning = new HashSet<>();
    }

    public void createInstance(final ResourceKey key, final InstanceType type) throws Exception {
        if (this.instances.containsKey(key)) {
            throw new InstanceAlreadyExistsException(key.toString());
        }

        final Instance instance;
        ServerWorld world = this.game.getServer().getWorldManager().getWorld(key).orElse(null);

        if (world == null) {
            world = this.game.getServer().getWorldManager().loadWorld(key).get();
            if (world == null) {
                throw new IOException(String.format("Failed to load instance '%s''!", key));
            }
        }

        world.getProperties().setKeepSpawnLoaded(true);
        world.getProperties().setSerializationBehavior(SerializationBehavior.NONE);

        instance = new Instance(this.game.getServer(), this, key, type);

        this.instances.put(world.getKey(), instance);
        this.instancesByTypes.computeIfAbsent(type, k -> new LinkedList<>()).add(instance);

        final InstanceMutatorPipeline pipeline = type.getMutatorPipeline();
        pipeline.mutate(instance, this.canUseFastPass.contains(world.getKey()));
    }

    public void startInstance(final ResourceKey key) throws UnknownInstanceException {
        final Instance instance = this.instances.get(key);
        if (instance == null) {
            throw new UnknownInstanceException(key.toString());
        }

        instance.advanceTo(Instance.State.PRE_START);
    }

    public void setWorldModified(final ResourceKey key, final boolean modified) {
        Royale.instance.getPlugin().getLogger().error("[Mutator] Setting fast pass availability for instance {} to {}.", key, !modified);

        if (modified) {
            this.canUseFastPass.remove(key);
        } else {
            this.canUseFastPass.add(key);
        }
    }

    public void endInstance(final ResourceKey key, final boolean force) throws UnknownInstanceException {
        final Instance instance = this.instances.get(key);
        if (instance == null) {
            throw new UnknownInstanceException(key.toString());
        }

        if (force) {
            instance.advanceTo(Instance.State.FORCE_STOP);
        } else {
            instance.advanceTo(Instance.State.PRE_END);
        }
    }

    void unloadInstance(final ResourceKey key) throws Exception {
        final Instance instance = this.instances.get(key);
        if (instance == null) {
            return;
        }

        final ServerWorld world = this.game.getServer().getWorldManager().getWorld(instance.getWorldKey()).orElse(null);

        if (world == null) {
            this.instances.remove(instance.getWorldKey());
            return;
        }

        final ServerWorld lobby = this.game.getServer().getWorldManager().getWorld(Constants.Map.Lobby.LOBBY_WORLD_KEY)
                .orElseThrow(() -> new RuntimeException("Lobby world was not found!"));

        // Move everyone out
        for (final ServerPlayer player : world.getPlayers()) {
            if (instance.isPlayerRegistered(player.getUniqueId())) {
                if (player.isRemoved()) {
                    this.forceRespawning.add(player.getUniqueId());
                    player.respawnPlayer();
                    this.convertToLobbyPlayer(player);
                    this.forceRespawning.remove(player.getUniqueId());
                }
                player.getInventory().clear();
            }

            player.setLocation(ServerLocation.of(lobby, lobby.getProperties().getSpawnPosition()));
        }

        this.instances.remove(instance.getWorldKey());
        final List<Instance> instancesForType = this.instancesByTypes.get(instance.getType());
        if (instancesForType != null) {
            instancesForType.remove(instance);

            if (instancesForType.size() == 0) {
                this.instancesByTypes.remove(instance.getType());
            }
        }

        if (!this.game.getServer().getWorldManager().unloadWorld(world).get()) {
            throw new RuntimeException(String.format("Failed to unload world for instance '%s'!", key));
        }

        this.setWorldModified(instance.getWorldKey(), false);
    }

    public Optional<Instance> getInstance(final ResourceKey key) {
        Objects.requireNonNull(key);
        return Optional.ofNullable(this.instances.get(key));
    }

    public Collection<Instance> getInstances(final InstanceType type) {
        Objects.requireNonNull(type);
        final List<Instance> instances = this.instancesByTypes.get(type);
        return Collections.unmodifiableCollection(instances == null ? Collections.emptyList() : instances);
    }

    public Collection<Instance> getAll() {
        return Collections.unmodifiableCollection(this.instances.values());
    }

    @Listener(order = Order.LAST)
    public void onServerSideConnectionPlayer(final ServerSideConnectionEvent event, @First final ServerPlayer player) {
        // We only care about these events
        if (!(event instanceof ServerSideConnectionEvent.Join || event instanceof ServerSideConnectionEvent.Disconnect)) {
            return;
        }

        final ServerWorld world = player.getWorld();
        final Instance instance = getInstance(world.getKey()).orElse(null);

        if (instance != null) {
            if (instance.isPlayerRegistered(player.getUniqueId())) {
                if (event instanceof ServerSideConnectionEvent.Join) {
                    // Player has played before, disconnected...let them come back as spectator
                    if (instance.isPlayerDead(player.getUniqueId())) {
                        instance.spectate(player);
                    }
                } else {
                    // Player disconnecting instantly means they forfeit
                    instance.disqualifyPlayer(player);
                    if (instance.isRoundOver()) {
                        instance.advanceTo(Instance.State.PRE_END);
                    }
                }
            }
        } else if (world.getKey().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
            this.convertToLobbyPlayer(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onMoveEntity(final MoveEntityEvent event, @First final ServerPlayer player) {
        final ServerWorld world = player.getWorld();

        final Instance instance = this.getInstance(world.getKey()).orElse(null);

        // We only care about inner-instance movement
        if (instance == null) {
            return;
        }

        // We only care about registered players
        if (!instance.isPlayerRegistered(player.getUniqueId())) {
            return;
        }

        // If a Player has already spawned, this means they are playing. See if the instance allows movement
        if (instance.isPlayerSpawned(player.getUniqueId()) && !instance.getState().canAnyoneMove()) {
            event.setCancelled(true);
        }
    }


    @Listener(order = Order.LAST)
    public void onChangeEntityWorld(final ChangeEntityWorldEvent event, @First final ServerPlayer player) {
        final ServerWorld fromWorld = event.getOriginalWorld();
        final ServerWorld toWorld = event.getDestinationWorld();

        final Instance fromInstance = this.getInstance(fromWorld.getKey()).orElse(null);
        final Instance toInstance = this.getInstance(toWorld.getKey()).orElse(null);

        // We don't care about non-instances from and to.
        if (fromInstance == null && toInstance == null && !toWorld.getKey().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
            return;
        }

        if (fromInstance != null) {
            // Switching out of instance means we kill them in the instance they left
            if (fromInstance.isPlayerRegistered(player.getUniqueId())) {
                fromInstance.disqualifyPlayer(player);
                if (fromInstance.isRoundOver()) {
                    fromInstance.advanceTo(Instance.State.PRE_END);
                }

                // Set them back to the default scoreboard
                this.convertToLobbyPlayer(player);
            } else {
                // Switching into an instance
                if (toInstance != null && toInstance.isPlayerRegistered(player.getUniqueId())) {
                    // Already dead here? Adjust them as a spectator
                    if (toInstance.isPlayerDead(player.getUniqueId())) {
                        toInstance.spectate(player);

                        player.setScoreboard(toInstance.getScoreboard().getHandle());
                    }

                } else if (toWorld.getKey().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                    // Going from a non-instance world to lobby
                    this.convertToLobbyPlayer(player);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onDestructEntity(final DestructEntityEvent.Death event, @Getter("getEntity") final ServerPlayer player) {
        final ServerWorld world = player.getWorld();
        final Instance instance = this.getInstance(world.getKey()).orElse(null);

        if (instance != null) {
            if (instance.isPlayerRegistered(player.getUniqueId())) {
                instance.disqualifyPlayer(player);
                player.respawnPlayer();
                if (instance.isRoundOver()) {
                    instance.advanceTo(Instance.State.PRE_END);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onRespawnPlayer(final RespawnPlayerEvent event, @Getter("getPlayer") final ServerPlayer player) {
        if (this.forceRespawning.contains(player.getUniqueId())) {
            return;
        }

        final ServerWorld fromWorld = event.getFromLocation().getWorld();
        ServerWorld toWorld = event.getToLocation().getWorld();
        final Instance fromInstance = getInstance(fromWorld.getKey()).orElse(null);
        Instance toInstance = this.getInstance(toWorld.getKey()).orElse(null);

        if (fromInstance != null && toInstance == null && fromInstance.isPlayerRegistered(player.getUniqueId()) && fromWorld.isLoaded()) {
            event.setToLocation(event.getFromLocation());
            event.setToRotation(event.getFromRotation());
        }

        toWorld = event.getToLocation().getWorld();
        toInstance = this.getInstance(toWorld.getKey()).orElse(null);

        if (fromInstance != null) {
            if (fromInstance.equals(toInstance)) {
                fromInstance.spectate(player);
                this.game.getServer().getScheduler().submit(Task.builder().execute(task -> fromInstance.spectate(player)).plugin(Royale.instance.getPlugin()).build());
            } else if (toInstance != null) {
                player.setScoreboard(toInstance.getScoreboard().getHandle());
            } else {
                player.setScoreboard(this.game.getServer().getServerScoreboard().orElse(null));
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onAttackPlayer(final AttackEntityEvent event, @Getter("getEntity") final ServerPlayer player) {
        final Instance instance = this.getInstance(player.getWorld().getKey()).orElse(null);

        if (instance != null && !instance.isPlayerRegistered(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onDamagePlayer(final DamageEntityEvent event, @Root final DamageSource source, @Getter("getEntity") final ServerPlayer player) {
        if (!(source.getType().equals(DamageTypes.FALL) || source.getType().equals(DamageTypes.VOID)) && player.getWorld().getKey()
                .equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onChangeSign(final ChangeSignEvent event, @Root final ServerPlayer player) {
        if (this.isTpSign(event.getText().get())) {
            if (player.hasPermission(Constants.Permissions.ADMIN)) {
                player.sendMessage(Identity.nil(), Component.text("Successfully created world teleportation sign!", NamedTextColor.GREEN));
                event.getText().set(0, event.getText().get(0).colorIfAbsent(NamedTextColor.AQUA));
            } else {
                player.sendMessage(Identity.nil(), Component.text("You do not have permission to create a world teleportation sign!", NamedTextColor.RED));
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onInteractByPlayer(final InteractEvent event, @Root final ServerPlayer player) {
        final ServerWorld world = player.getWorld();
        final Instance instance = getInstance(world.getKey()).orElse(null);

        if (instance != null && !instance.getState().canAnyoneInteract() && instance.isPlayerRegistered(player.getUniqueId())) {
            event.setCancelled(true);
        }

        if (event instanceof InteractBlockEvent.Secondary) {
            final BlockSnapshot block = ((InteractBlockEvent.Secondary) event).getBlock();

            block.getLocation().flatMap(Location::getBlockEntity).flatMap(t -> t.get(Keys.SIGN_LINES)).ifPresent(lines -> {
                if (this.isTpSign(lines)) {
                    final String name = PlainComponentSerializer.plain().serialize(lines.get(1));
                    final Optional<Instance> optInstance = this.getInstance(ResourceKey.resolve(name));
                    if (optInstance.isPresent()) {
                        if (!optInstance.get().canRegisterMorePlayers()) {
                            player.sendMessage(Identity.nil(), Component.text("World is full!", NamedTextColor.RED));
                            return;
                        }
                        player.sendMessage(Identity.nil(), Component.text("Joining world " + name, NamedTextColor.GREEN));
                        optInstance.get().registerPlayer(player);
                        optInstance.get().spawnPlayer(player);
                    } else {
                        if (name.equals("")) {
                            final Collection<Instance> instances = this.getAll();
                            if (instances.size() != 1) {
                                player.sendMessage(Identity.nil(),
                                        Component.text(String.format("Unable to automatically join select - there are %s to choose from.", instances.size()),
                                                NamedTextColor.RED));
                                return;

                            }
                            final Instance realInstance = Iterables.getFirst(instances, null);
                            if (realInstance != null) {
                                realInstance.registerPlayer(player);
                                realInstance.spawnPlayer(player);
                            }
                        }
                        player.sendMessage(Identity.nil(), Component.text(String.format("World %s isn't up yet!", name), NamedTextColor.RED));
                    }
                }
            });
        }
    }

    @Listener
    public void onChangeBlock(final ChangeBlockEvent event, @First final ServerPlayer player) {
        for (final Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            final BlockSnapshot snapshot = transaction.getFinal();
            if (snapshot.getWorld().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                transaction.setValid(false);
            }
        }

        final Set<ResourceKey> uniqueWorlds = event.getTransactions().stream()
                .map(x -> x.getOriginal().getWorld())
                .collect(Collectors.toSet());
        for (final ResourceKey name : uniqueWorlds) {
            if (!this.getInstance(name).isPresent() && this.canUseFastPass.contains(name)) {
                this.setWorldModified(name, true);
                player.sendMessage(Identity.nil(), Component.text(String.format(
                        "You have modified the world '%s' - mutators will take longer to run the next time an instance of this map is started.\n" +
                                "If you make any modifications outside of the game, make sure to run '/worldmodified %s' so that your changes are "
                                + "detected.",
                        name, name), NamedTextColor.YELLOW));
            }
        }
    }

    private void convertToLobbyPlayer(final ServerPlayer player) {
        player.setScoreboard(this.game.getServer().getServerScoreboard().orElse(null));
        Utils.resetPlayer(player);
    }

    private boolean isTpSign(final List<Component> lines) {
        return lines.size() != 0 && PlainComponentSerializer.plain().serialize(lines.get(0)).equalsIgnoreCase(Constants.Map.Lobby.SIGN_HEADER);
    }
}
