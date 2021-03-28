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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
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
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.instance.exception.InstanceAlreadyExistsException;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;
import org.spongepowered.royale.instance.gen.InstanceMutatorPipeline;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InstanceManager {

    private final Server server;
    private final Map<ResourceKey, Instance> instances;
    private final Map<InstanceType, List<Instance>> instancesByTypes;

    public InstanceManager(final Server server) {
        this.server = server;
        this.instances = new HashMap<>();
        this.instancesByTypes = new HashMap<>();
    }

    public Instance createInstance(final ResourceKey key, final InstanceType type, ServerLocation signLoc) {
        if (this.instances.containsKey(key)) {
            throw new InstanceAlreadyExistsException(key.toString());
        }

        final Instance instance;
        ServerWorld world = this.server.worldManager().world(key).orElse(null);

        if (world == null) {
            world = this.server.worldManager().loadWorld(key).getNow(null);
            if (world == null) {
                throw new RuntimeException(String.format("Failed to load instance '%s''!", key));
            }
        }

        //world.getProperties().setKeepSpawnLoaded(true);
        world.properties().setSerializationBehavior(SerializationBehavior.AUTOMATIC_METADATA_ONLY);

        instance = new Instance(this.server, this, key, type);
        instance.assign(signLoc);

        this.instances.put(world.key(), instance);
        this.instancesByTypes.computeIfAbsent(type, k -> new LinkedList<>()).add(instance);

        final InstanceMutatorPipeline pipeline = type.getMutatorPipeline();
        pipeline.mutate(instance);
        return instance;
    }

    public void startInstance(final ResourceKey key) throws UnknownInstanceException {
        final Instance instance = this.instances.get(key);
        if (instance == null) {
            throw new UnknownInstanceException(key.toString());
        }

        instance.advanceTo(Instance.State.PRE_START);
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

        final ServerWorld world = this.server.worldManager().world(instance.getWorldKey()).orElse(null);

        if (world == null) {
            this.instances.remove(instance.getWorldKey());
            return;
        }

        final ServerWorld lobby = this.server.worldManager().world(Constants.Map.Lobby.LOBBY_WORLD_KEY)
                .orElseThrow(() -> new RuntimeException("Lobby world was not found!"));

        // Move everyone out
        for (final ServerPlayer player : world.players()) {
            if (instance.isPlayerRegistered(player.uniqueId())) {
                this.convertToLobbyPlayer(player);
                player.inventory().clear();
            }

            player.setLocation(ServerLocation.of(lobby, lobby.properties().spawnPosition()));
        }

        this.instances.remove(instance.getWorldKey());
        final List<Instance> instancesForType = this.instancesByTypes.get(instance.getType());
        if (instancesForType != null) {
            instancesForType.remove(instance);

            if (instancesForType.size() == 0) {
                this.instancesByTypes.remove(instance.getType());
            }
        }

        instance.updateSign();

        if (!this.server.worldManager().unloadWorld(world).get()) {
            throw new RuntimeException(String.format("Failed to unload world for instance '%s'!", key));
        }
    }

    public Optional<Instance> getInstance(final ResourceKey key) {
        Objects.requireNonNull(key);
        return Optional.ofNullable(this.instances.get(key));
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

        final ServerWorld world = player.world();
        final Instance instance = this.getInstance(world.key()).orElse(null);

        if (instance != null) {
            if (instance.isPlayerRegistered(player.uniqueId())) {
                if (event instanceof ServerSideConnectionEvent.Join) {
                    // Player has played before, disconnected...let them come back as spectator
                    if (instance.isPlayerDead(player.uniqueId())) {
                        instance.spectate(player);
                    }
                } else {
                    // Player disconnecting instantly means they forfeit
                    instance.disqualifyPlayer(player);
                    if (instance.isRoundOver()) {
                        if (world.players().isEmpty()) {
                            instance.advanceTo(Instance.State.FORCE_STOP);
                        } else {
                            instance.advanceTo(Instance.State.PRE_END);
                        }
                    }
                }
            }
        } else {
            if (world.key().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                this.convertToLobbyPlayer(player);
            } else if (!player.hasPlayedBefore()) {
                this.server.scheduler().submit(Task.builder()
                        .delay(Ticks.of(0))
                        .execute(() -> this.server.worldManager().world(Constants.Map.Lobby.LOBBY_WORLD_KEY).ifPresent(w -> {
                            if (player.isOnline()) {
                                player.setLocation(ServerLocation.of(w, w.properties().spawnPosition()));
                                InstanceManager.this.convertToLobbyPlayer(player);
                            }
                        }))
                        .plugin(Royale.instance.getPlugin())
                        .build()
                );
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onMoveEntity(final MoveEntityEvent event, @First final ServerPlayer player) {
        final ServerWorld world = player.world();

        final Instance instance = this.getInstance(world.key()).orElse(null);

        // We only care about inner-instance movement
        if (instance == null) {
            return;
        }

        // We only care about registered players
        if (!instance.isPlayerRegistered(player.uniqueId())) {
            return;
        }

        // If a Player has already spawned, this means they are playing. See if the instance allows movement
        if (instance.isPlayerSpawned(player.uniqueId()) && !instance.isPlayerDead(player.uniqueId()) && !instance.getState().canAnyoneMove()) {
            event.setCancelled(true);
        }
    }


    @Listener(order = Order.LAST)
    public void onChangeEntityWorld(final ChangeEntityWorldEvent event, @First final ServerPlayer player) {
        final ServerWorld fromWorld = event.originalWorld();
        final ServerWorld toWorld = event.destinationWorld();

        final Instance fromInstance = this.getInstance(fromWorld.key()).orElse(null);
        final Instance toInstance = this.getInstance(toWorld.key()).orElse(null);

        // We don't care about non-instances from and to.
        if (fromInstance == null && toInstance == null && !toWorld.key().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
            return;
        }

        if (fromInstance != null) {

            player.setScoreboard(this.server.serverScoreboard().get());

            // Switching out of instance means we kill them in the instance they left
            if (fromInstance.isPlayerRegistered(player.uniqueId())) {
                fromInstance.disqualifyPlayer(player);
                if (fromInstance.isRoundOver()) {
                    fromInstance.advanceTo(Instance.State.PRE_END);
                }

                // Set them back to the default scoreboard
                this.convertToLobbyPlayer(player);
            } else {
                // Switching into an instance
                if (toInstance != null && toInstance.isPlayerRegistered(player.uniqueId())) {
                    // Already dead here? Adjust them as a spectator
                    if (toInstance.isPlayerDead(player.uniqueId())) {
                        toInstance.spectate(player);

                        player.setScoreboard(toInstance.getScoreboard().getHandle());
                    }

                } else {

                    // Going from a non-instance world to lobby
                    this.convertToLobbyPlayer(player);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onDestructEntity(final DestructEntityEvent.Death event, @Getter("entity") final ServerPlayer player) {
        final ServerWorld world = player.world();
        final Instance instance = this.getInstance(world.key()).orElse(null);

        if (instance != null) {
            if (instance.isPlayerRegistered(player.uniqueId())) {
                event.setCancelled(true);
                player.offer(Keys.HEALTH, 20.0);
                player.transform(Keys.POTION_EFFECTS, list -> {
                    list.add(PotionEffect.of(PotionEffectTypes.NIGHT_VISION, 1, 1000000000));
                    return list;
                });
                instance.disqualifyPlayer(player);
                if (instance.isRoundOver()) {
                    instance.advanceTo(Instance.State.PRE_END);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onRespawnPlayerSelectWorld(final RespawnPlayerEvent.SelectWorld event, @Getter("entity") final ServerPlayer player,
            @Getter("originalWorld") final ServerWorld fromWorld, @Getter("destinationWorld") final ServerWorld toWorld) {

        final Instance fromInstance = this.getInstance(fromWorld.key()).orElse(null);
        Instance toInstance = this.getInstance(toWorld.key()).orElse(null);

        if (fromInstance != null && toInstance == null && fromInstance.isPlayerRegistered(player.uniqueId()) && fromWorld.isLoaded()) {
            event.setDestinationWorld(fromWorld);
        }
    }

    @Listener(order = Order.LAST)
    public void onRespawnPlayer(final RespawnPlayerEvent.Recreate event, @Getter("recreatedPlayer") final ServerPlayer player,
            @Getter("originalWorld") final ServerWorld fromWorld, @Getter("destinationWorld") final ServerWorld toWorld) {

        final Instance fromInstance = this.getInstance(fromWorld.key()).orElse(null);
        Instance toInstance = this.getInstance(toWorld.key()).orElse(null);

        if (fromInstance != null && toInstance == null && fromInstance.isPlayerRegistered(player.uniqueId()) && fromWorld.isLoaded()) {
            event.setDestinationPosition(event.originalPosition());
        }

        toInstance = this.getInstance(toWorld.key()).orElse(null);

        if (fromInstance != null) {
            if (fromInstance.equals(toInstance)) {
                fromInstance.spectate(player);
            } else if (toInstance != null) {
                player.setScoreboard(toInstance.getScoreboard().getHandle());
            } else {
                player.setScoreboard(this.server.serverScoreboard().orElse(null));
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onAttackPlayer(final AttackEntityEvent event, @Getter("entity") final ServerPlayer player) {
        final Instance instance = this.getInstance(player.world().key()).orElse(null);

        if (instance != null && !instance.isPlayerRegistered(player.uniqueId())) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onDamagePlayer(final DamageEntityEvent event, @Root final DamageSource source, @Getter("entity") final ServerPlayer player) {
        if (!(source.type().equals(DamageTypes.FALL.get()) || source.type().equals(DamageTypes.VOID.get())) && player.world().key()
                .equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onChangeSign(final ChangeSignEvent event, @Root final ServerPlayer player) {
        if (this.isTeleportSign(event.text().get())) {
            if (player.hasPermission(Constants.Permissions.ADMIN + ".create.sign")) {

                final String namespace = SpongeComponents.plainSerializer().serialize(event.text().get(1));
                final String value = SpongeComponents.plainSerializer().serialize(event.text().get(2));
                final String type = SpongeComponents.plainSerializer().serialize(event.text().get(3));
                final ResourceKey typeKey = ResourceKey.of("royale", type);

                final ResourceKey worldKey = ResourceKey.of(namespace, value);
                final Optional<ServerWorld> world = Sponge.server().worldManager().world(worldKey);
                if (!Sponge.server().worldManager().world(worldKey).isPresent()) {
                    player.sendMessage(Identity.nil(), Component.text(String.format("Could not find the world %s!", worldKey), NamedTextColor.RED));
                    return;
                }

                final Optional<InstanceType> instanceType = Constants.Plugin.INSTANCE_TYPE.get().findValue(typeKey);
                if (!instanceType.isPresent()) {
                    player.sendMessage(Identity.nil(), Component.text(String.format("Could not find the instance type %s!", typeKey), NamedTextColor.RED));
                    return;
                }

                event.text().set(0, event.text().get(0).color(NamedTextColor.AQUA));

                event.text().set(1, world.get().properties().displayName().orElse(Component.text(worldKey.asString())));
                event.text().set(2, Component.empty());
                event.sign().offer(RoyaleData.WORLD, worldKey.asString());
                event.text().set(3, Component.text(instanceType.get().name()));

                event.sign().offer(RoyaleData.TYPE, typeKey.asString());

                player.sendMessage(Identity.nil(), Component.text("Successfully created world teleportation sign!", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Identity.nil(), Component.text("You do not have permission to create a world teleportation sign!", NamedTextColor.RED));
                event.setCancelled(true);
            }
        }
    }

    @Listener
    //TODO this is wrong. It needs a broader event
    public void onInteractByPlayer(final InteractBlockEvent.Secondary event, @Root final ServerPlayer player) {
//        final ServerWorld world = player.world();
//        final Instance instance = this.getInstance(world.key()).orElse(null);
//
//        if (instance != null && !instance.getState().canAnyoneInteract() && instance.isPlayerRegistered(player.uniqueId())) {
//            event.setCancelled(true);
//        }

        final BlockSnapshot block = event.block();
        block.location().flatMap(Location::blockEntity).filter(Sign.class::isInstance).ifPresent(sign -> {
            final Optional<ResourceKey> worldKey = sign.get(RoyaleData.WORLD).map(ResourceKey::resolve);
            final Optional<ResourceKey> typeKey = sign.get(RoyaleData.TYPE).map(ResourceKey::resolve);
            if (worldKey.isPresent() && typeKey.isPresent()) {

                final Optional<Instance> optInstance = this.getInstance(worldKey.get());
                if (optInstance.isPresent()) {
                    final Instance instance = optInstance.get();
                    if (!instance.canRegisterMorePlayers()) {
                        player.sendActionBar(Component.text("World is full!", NamedTextColor.RED));
                    } else {
                        player.sendActionBar(Component.text(String.format("Joining world '%s'", worldKey.get()), NamedTextColor.GREEN));
                        if (instance.registerPlayer(player)) {
                            instance.spawnPlayer(player);
                        }
                    }


                    instance.updateSign();


                } else {
                    final Optional<InstanceType> type = Constants.Plugin.INSTANCE_TYPE.get().findValue(typeKey.get());
                    player.sendActionBar(Component.text(String.format("World '%s' isn't up yet! Creating instance...", worldKey.get()), NamedTextColor.RED));
                    sign.transform(Keys.SIGN_LINES, lines -> {
                        lines.set(2, Component.text("creating Instance", NamedTextColor.YELLOW));
                        return lines;
                    });

                    this.server.worldManager().loadWorld(worldKey.get()).thenAccept(world -> {
                        Sponge.server().scheduler().submit(Task.builder()
                                .execute(() -> this.createInstance(sign, worldKey.get(), type.get()))
                                .plugin(Royale.instance.getPlugin()).build());
                    });

                }
            }

        });
    }

    private void createInstance(BlockEntity sign, ResourceKey worldKey, InstanceType type) {
        final Instance instance = this.createInstance(worldKey, type, sign.serverLocation());
        final int spawns = instance.spawns();
        instance.updateSign();
    }

    @Listener
    public void onChangeBlock(final ChangeBlockEvent.All event, @First final ServerPlayer player) {
        for (final Transaction<BlockSnapshot> transaction : event.transactions()) {
            final BlockSnapshot snapshot = transaction.finalReplacement();
            if (!player.hasPermission(Constants.Permissions.ADMIN + ".lobby.edit") && snapshot.world()
                    .equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                transaction.setValid(false);
            }
        }
    }

    private void convertToLobbyPlayer(final ServerPlayer player) {
        player.setScoreboard(this.server.serverScoreboard().orElse(null));
        Utils.resetPlayer(player);
    }

    private boolean isTeleportSign(final List<Component> lines) {
        return lines.size() != 0 && SpongeComponents.plainSerializer().serialize(lines.get(0)).equalsIgnoreCase(Constants.Map.Lobby.JOIN_SIGN_HEADER);
    }
}
