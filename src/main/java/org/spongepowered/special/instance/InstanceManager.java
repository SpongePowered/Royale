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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
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
import org.spongepowered.api.world.SerializationBehaviors;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.dimension.DimensionType;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.special.Constants;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.exception.InstanceAlreadyExistsException;
import org.spongepowered.special.instance.exception.UnknownInstanceException;
import org.spongepowered.special.instance.gen.InstanceMutatorPipeline;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class InstanceManager {

    // World Name -> Instance
    private final Map<ResourceKey, Instance> instances = new HashMap<>();

    // Instance Type -> List(Instances)
    private final Map<InstanceType, List<Instance>> instancesByTypes = new HashMap<>();

    private final Set<ResourceKey> canUseFastPass = new HashSet<>();

    private final Set<UUID> forceRespawning = new HashSet<>();

    public void createInstance(ResourceKey instanceName, InstanceType type) throws Exception {
        if (this.instances.containsKey(instanceName)) {
            throw new InstanceAlreadyExistsException(instanceName.toString());
        }

        Instance instance;
        ServerWorld world = Sponge.getServer().getWorldManager().getWorld(instanceName).orElse(null);

        if (world == null) {
            world = Sponge.getServer().getWorldManager().loadWorld(instanceName).orElse(null);
            if (world == null) {
                throw new IOException("Failed to load instance [" + instanceName + "]!");
            }
        }

        world.getProperties().setKeepSpawnLoaded(true);
        world.getProperties().setSerializationBehavior(SerializationBehaviors.NONE);

        instance = new Instance(instanceName, type, world);

        this.instances.put(world.getKey(), instance);
        List<Instance> instances = this.instancesByTypes.get(type);
        if (instances == null) {
            instances = new LinkedList<>();
            this.instancesByTypes.put(type, instances);

        }
        instances.add(instance);

        final InstanceMutatorPipeline pipeline = type.getMutatorPipeline();
        pipeline.mutate(instance, this.canUseFastPass.contains(world.getKey()));
    }

    /**
     * Advances the {@link Instance} associated by name to the pre start state.
     *
     * @param instanceName The name of the instance
     * @throws UnknownInstanceException If the instance isn't known
     */
    public void startInstance(ResourceKey instanceName) throws UnknownInstanceException {
        final Instance instance = this.instances.get(instanceName);
        if (instance == null) {
            throw new UnknownInstanceException(instanceName.toString());
        }

        instance.advanceTo(Instance.State.PRE_START);
    }

    public void setWorldModified(ResourceKey instanceName, boolean modified) {
        Special.instance.getLogger().error("[Mutator] Setting fast pass availability for instance {} to {}.", instanceName, !modified);

        if (modified) {
            this.canUseFastPass.remove(instanceName);
        } else {
            this.canUseFastPass.add(instanceName);
        }
    }

    public void endInstance(ResourceKey instanceName, boolean force) throws UnknownInstanceException {
        final Instance instance = this.instances.get(instanceName);
        if (instance == null) {
            throw new UnknownInstanceException(instanceName.toString());
        }

        if (force) {
            instance.advanceTo(Instance.State.FORCE_STOP);
        } else {
            instance.advanceTo(Instance.State.PRE_END);
        }
    }

    void unloadInstance(Instance instance) {
        final Server server = Sponge.getServer();
        final ServerWorld world = instance.getHandle().orElse(null);

        if (world == null) {
            this.instances.remove(instance.getName());
            return;
        }

        final ServerWorld lobby = server.getWorldManager().getWorld(Constants.Map.Lobby.DEFAULT_LOBBY_NAME)
                .orElseThrow(() -> new RuntimeException("Lobby world was not found!"));

        // Move everyone out
        for (ServerPlayer player : world.getPlayers()) {
            if (instance.getRegisteredPlayers().contains(player.getUniqueId())) {
                if (player.isRemoved()) {
                    this.forceRespawning.add(player.getUniqueId());
                    player.respawnPlayer();
                    this.giveLobbySetting(player);
                    this.forceRespawning.remove(player.getUniqueId());
                }
                player.getInventory().clear();
            }

            player.setLocation(ServerLocation.of(lobby, lobby.getProperties().getSpawnPosition()));
        }

        this.instances.remove(instance.getName());
        final List<Instance> instancesForType = this.instancesByTypes.get(instance.getType());
        if (instancesForType != null) {
            instancesForType.remove(instance);

            if (instancesForType.size() == 0) {
                this.instancesByTypes.remove(instance.getType());
            }
        }

        if (!server.getWorldManager().unloadWorld(world)) {
            throw new RuntimeException("Failed to unload instance world!"); // TODO Specialized exception
        }

        this.setWorldModified(instance.getName(), false);
    }

    public Optional<Instance> getInstance(ResourceKey instanceName) {
        checkNotNull(instanceName);
        return Optional.ofNullable(this.instances.get(instanceName));
    }

    public Collection<Instance> getInstances(InstanceType type) {
        checkNotNull(type);
        List<Instance> instances = this.instancesByTypes.get(type);
        if (instances == null) {
            instances = new LinkedList<>();
        }
        return Collections.unmodifiableCollection(instances);
    }

    public Collection<Instance> getAll() {
        return Collections.unmodifiableCollection(this.instances.values());
    }

    @Listener(order = Order.LAST)
    public void onClientConnection(ServerSideConnectionEvent event, @First ServerPlayer player) {
        // We only care about these events
        if (!(event instanceof ServerSideConnectionEvent.Join || event instanceof ServerSideConnectionEvent.Disconnect)) {
            return;
        }

        final ServerWorld world = player.getWorld();
        final Instance instance = getInstance(world.getKey()).orElse(null);

        if (instance != null) {
            if (instance.getRegisteredPlayers().contains(player.getUniqueId())) {
                if (event instanceof ServerSideConnectionEvent.Join) {
                    // Player has played before, disconnected...let them come back as spectator
                    if (instance.getPlayerDeaths().containsKey(player.getUniqueId())) {
                        instance.convertPlayerToSpectator(player);
                    }
                } else {
                    // Player disconnecting instantly means they forfeit
                    instance.disqualifyPlayer(player, Cause.of(EventContext.empty(), instance));
                    if (instance.isRoundOver()) {
                        instance.advanceTo(Instance.State.PRE_END);
                    }
                }
            }
        } else if (world.getKey().equals(Constants.Map.Lobby.DEFAULT_LOBBY_NAME)) {
            this.giveLobbySetting(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onMoveEntity(MoveEntityEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer)) {
            // We only care about Players.
            return;
        }

        final ServerPlayer player = (ServerPlayer) event.getEntity();
        final ServerWorld world = player.getWorld();

        final Instance instance = getInstance(world.getKey()).orElse(null);

        // Movement inner-instance
        if (instance != null) {
            // We only care about registered players
            if (instance.getRegisteredPlayers().contains(player.getUniqueId())) {
                // If a Player has already spawned, this means they are playing. See if the instance allows movement
                if (instance.getPlayerSpawns().containsKey(player.getUniqueId()) && !instance.getState().canAnyoneMove()) {
                    if (!event.getOriginalPosition().equals(event.getDestinationPosition())) {
                        // Don't cancel the event, to allow people to look around.
                        event.setDestinationPosition(event.getOriginalPosition());
                    }
                }
            }
        }
    }


    @Listener(order = Order.LAST)
    public void onChangeEntityWorld(ChangeEntityWorldEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer)) {
            // We only care about Players.
            return;
        }

        final ServerPlayer player = (ServerPlayer) event.getEntity();
        final ServerWorld fromWorld = event.getOriginalWorld();
        final ServerWorld toWorld = event.getDestinationWorld();

        final Instance fromInstance = getInstance(fromWorld.getKey()).orElse(null);
        final Instance toInstance = getInstance(toWorld.getKey()).orElse(null);

        // We don't care about non-instances from and to.
        if (fromInstance == null && toInstance == null && !toWorld.getKey().equals(Constants.Map.Lobby.DEFAULT_LOBBY_NAME)) {
            return;
        }

        if (fromInstance != null) {
            // Switching out of instance means we kill them in the instance they left
            if (fromInstance.getRegisteredPlayers().contains(player.getUniqueId())) {
                fromInstance.disqualifyPlayer(player, Cause.of(EventContext.empty(), fromInstance));
                if (fromInstance.isRoundOver()) {
                    fromInstance.advanceTo(Instance.State.PRE_END);
                }

                // Set them back to the default scoreboard
                this.giveLobbySetting(player);
            } else {
                // Switching into an instance
                if (toInstance != null && toInstance.getRegisteredPlayers().contains(player.getUniqueId())) {
                    // Already dead here? Adjust them as a spectator
                    if (toInstance.getPlayerDeaths().containsKey(player.getUniqueId())) {
                        toInstance.convertPlayerToSpectator(player);

                        player.setScoreboard(toInstance.getScoreboard().getHandle());
                    }

                } else if (toWorld.getKey().equals(Constants.Map.Lobby.DEFAULT_LOBBY_NAME)) {
                    // Going from a non-instance world to lobby
                    this.giveLobbySetting(player);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onDestructEntity(DestructEntityEvent.Death event, @Getter("getEntity") ServerPlayer player) {
        final ServerWorld world = player.getWorld();
        final Instance instance = getInstance(world.getKey()).orElse(null);

        if (instance != null) {
            if (instance.getRegisteredPlayers().contains(player.getUniqueId())) {
                instance.disqualifyPlayer(player, event.getCause());
                player.respawnPlayer();
                if (instance.isRoundOver()) {
                    instance.advanceTo(Instance.State.PRE_END);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onRespawnPlayer(RespawnPlayerEvent event, @Getter("getPlayer") ServerPlayer player) {
        if (this.forceRespawning.contains(player.getUniqueId())) {
            return;
        }

        final ServerWorld fromWorld = event.getFromLocation().getWorld();
        ServerWorld toWorld = event.getToLocation().getWorld();
        final Instance fromInstance = getInstance(fromWorld.getKey()).orElse(null);
        Instance toInstance = getInstance(toWorld.getKey()).orElse(null);

        if (fromInstance != null && toInstance == null) {
            if (fromInstance.getRegisteredPlayers().contains(player.getUniqueId())) {
                // Sometimes an instance is using a dimension that doesn't allow respawns. Override that logic so long as the instance world is
                // still loaded
                final DimensionType dimension = fromWorld.getDimensionType();
                if (!dimension.allowsPlayerRespawns() && fromWorld.isLoaded()) {
                    event.setToLocation(event.getFromLocation());
                    event.setToRotation(event.getFromRotation());
                }
            }
        }

        toWorld = event.getToLocation().getWorld();
        toInstance = getInstance(toWorld.getKey()).orElse(null);

        if (fromInstance != null) {
            if (toInstance != null && fromInstance.equals(toInstance)) {
                fromInstance.convertPlayerToSpectator(player);
                Sponge.getServer().getScheduler().submit(Task.builder().execute(task -> fromInstance.convertPlayerToSpectator(player)).plugin(Special.instance).build());
            } else if (toInstance != null) {
                player.setScoreboard(toInstance.getScoreboard().getHandle());
            } else {
                player.setScoreboard(Sponge.getServer().getServerScoreboard().orElse(null));
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onAttackEntity(AttackEntityEvent event) {
        final Entity victim = event.getEntity();
        final ServerWorld world = victim.getServerLocation().getWorld();
        final Instance instance = getInstance(world.getKey()).orElse(null);

        if (instance != null) {
            if (victim instanceof Player && !instance.getRegisteredPlayers().contains(victim.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onDamageEntity(DamageEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            final ServerPlayer player = (ServerPlayer) event.getEntity();

            final Object root = event.getCause().root();

            if (root instanceof DamageSource) {
                final DamageSource source = (DamageSource) root;

                if (!(source.getType().equals(DamageTypes.FALL) || source.getType().equals(DamageTypes.VOID)) && player.getWorld().getName()
                        .equalsIgnoreCase(Constants.Map.Lobby.DEFAULT_LOBBY_NAME)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Listener
    public void onChangeSign(ChangeSignEvent event, @Root Player player) {
        if (this.isTpSign(event.getText().lines().get())) {
            if (player.hasPermission(Constants.Permissions.ADMIN)) {
                player.sendMessage(Text.of(TextColors.GREEN, "Successfully created world teleportation sign!"));
                event.getText().setElement(0, Text.of(TextColors.AQUA, event.getText().get(0).orElse(null)));
            } else {
                player.sendMessage(Text.of(TextColors.RED, "You don't have permission to create a world teleportation sign!"));
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onInteract(InteractEvent event, @Root ServerPlayer player) {

        final ServerWorld world = player.getWorld();
        final Instance instance = getInstance(world.getKey()).orElse(null);

        if (instance != null && !instance.getState().canAnyoneInteract() && instance.getRegisteredPlayers().contains(player.getUniqueId())) {
            event.setCancelled(true);
        }

        if (event instanceof InteractBlockEvent.Secondary.MainHand) {
            final BlockSnapshot block = ((InteractBlockEvent.Secondary.MainHand) event).getTargetBlock();

            block.getLocation().flatMap(Location::getBlockEntity).flatMap(t -> t.get(Keys.SIGN_LINES)).ifPresent(lines -> {
                if (this.isTpSign(lines)) {
                    String name = PlainComponentSerializer.plain().serialize(lines.get(1));
                    Optional<Instance> optInstance = Special.instance.getInstanceManager().getInstance(name);
                    if (optInstance.isPresent()) {
                        if (!optInstance.get().canRegisterMorePlayers()) {
                            player.sendMessage(TextComponent.of("World is full!", NamedTextColor.RED));
                            return;
                        }
                        player.sendMessage(TextComponent.of("Joining world " + name, NamedTextColor.GREEN));
                        optInstance.get().registerPlayer(player);
                        optInstance.get().spawnPlayer(player);
                    } else {
                        if (name.equals("")) {
                            Collection<Instance> instances = Special.instance.getInstanceManager().getAll();
                            if (instances.size() != 1) {
                                player.sendMessage(
                                        TextComponent.of(String.format("Unable to automatically join select - there are %s to choose from.",
                                                                                        instances.size()), NamedTextColor.RED));
                                return;

                            }
                            final Instance realInstance = Iterables.getFirst(instances, null);
                            if (realInstance != null) {
                                realInstance.registerPlayer(player);
                                realInstance.spawnPlayer(player);
                            }
                        }
                        player.sendMessage(TextComponent.of(String.format("World %s isn't up yet!", name), NamedTextColor.RED));
                    }
                }
            });
        }
    }

    @Listener
    public void onChangeBlock(ChangeBlockEvent event, @First Player player) {
        ResourceKey name = event.getTargetWorld().getName();
        if (name.equalsIgnoreCase(Constants.Map.Lobby.DEFAULT_LOBBY_NAME)) {
            event.setCancelled(true);
            return;
        }

        if (!this.getInstance(name).isPresent() && this.canUseFastPass.contains(name)) {
            this.setWorldModified(name, true);
            player.sendMessage(TextComponent.of(String.format(
                                "You have modified the world '%s' - mutators will take longer to run the next time an instance of this map is started.\n" +
                                        "If you make any modifications outside of the game, make sure to run '/worldmodified %s' so that your changes are "
                                        + "detected.",
                                name, name), NamedTextColor.YELLOW));
        }
    }

    private void giveLobbySetting(Player player) {
        player.setScoreboard(Sponge.getServer().getServerScoreboard().orElse(null));
        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL.get());
        Utils.resetHealthHungerAndPotions(player);
    }

    private boolean isTpSign(List<Component> lines) {
        return lines.size() != 0 && PlainComponentSerializer.plain().serialize(lines.get(0)).equalsIgnoreCase(Constants.Map.Lobby.SIGN_HEADER);
    }
}
