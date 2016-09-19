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

import com.flowpowered.noise.module.source.Const;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.api.world.SerializationBehaviors;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Constants;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.exception.InstanceAlreadyExistsException;
import org.spongepowered.special.instance.exception.UnknownInstanceException;
import org.spongepowered.special.instance.gen.InstanceMutatorPipeline;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InstanceManager {

    // World Name -> Instance
    private final Map<String, Instance> instances = new HashMap<>();

    // Instance Type -> List(Instances)
    private final Map<InstanceType, List<Instance>> instancesByTypes = new HashMap<>();

    public void createInstance(String instanceName, InstanceType type) throws Exception {
        if (this.instances.containsKey(instanceName)) {
            throw new InstanceAlreadyExistsException(instanceName);
        }

        Instance instance;
        World world = Sponge.getServer().getWorld(instanceName).orElse(null);

        if (world == null) {
            world = Sponge.getServer().loadWorld(instanceName).orElse(null);
            if (world == null) {
                throw new IOException("Failed to load instance [" + instanceName + "]!");
            }

            world.setKeepSpawnLoaded(true);
            world.setSerializationBehavior(SerializationBehaviors.NONE);

            instance = new Instance(instanceName, type, world);

            this.instances.put(world.getName(), instance);
            List<Instance> instances = this.instancesByTypes.get(type);
            if (instances == null) {
                instances = new LinkedList<>();
                this.instancesByTypes.put(type, instances);

            }
            instances.add(instance);

            final InstanceMutatorPipeline pipeline = type.getMutatorPipeline();
            pipeline.mutate(instance);
        } else {
            world.setKeepSpawnLoaded(true);
            world.setSerializationBehavior(SerializationBehaviors.NONE);

            instance = new Instance(instanceName, type, world);

            this.instances.put(world.getName(), instance);
            List<Instance> instances = this.instancesByTypes.get(type);
            if (instances == null) {
                instances = new LinkedList<>();
                this.instancesByTypes.put(type, instances);

            }
            instances.add(instance);

            final InstanceMutatorPipeline pipeline = type.getMutatorPipeline();
            pipeline.mutate(instance);
        }
    }

    /**
     * Advances the {@link Instance} associated by name to the pre start state.
     *
     * @param instanceName The name of the instance
     * @throws UnknownInstanceException If the instance isn't known
     */
    public void startInstance(String instanceName) throws UnknownInstanceException {
        final Instance instance = this.instances.get(instanceName);
        if (instance == null) {
            throw new UnknownInstanceException(instanceName);
        }

        instance.advanceTo(Instance.State.PRE_START);
    }

    public void endInstance(String instanceName, boolean force) throws UnknownInstanceException {
        final Instance instance = this.instances.get(instanceName);
        if (instance == null) {
            throw new UnknownInstanceException(instanceName);
        }

        if (force) {
            instance.advanceTo(Instance.State.FORCE_STOP);
        } else {
            instance.advanceTo(Instance.State.PRE_END);
        }
    }

    void unloadInstance(Instance instance) {
        final Server server = Sponge.getServer();
        final World world = instance.getHandle().orElse(null);

        if (world == null) {
            return;
        }

        final World lobby = server.getWorld(Constants.Map.Lobby.DEFAULT_LOBBY_NAME).orElseThrow(() -> new RuntimeException("Lobby world was not "
                + "found!"));

        // Move everyone out
        for (Player player : world.getPlayers()) {
            if (instance.getRegisteredPlayers().contains(player.getUniqueId())) {
                player.getInventory().clear();
            }

            Special.instance.getLogger().error(player.getName());
            player.setLocation(lobby.getSpawnLocation());
        }

        this.instances.remove(instance.getName());
        final List<Instance> instancesForType = this.instancesByTypes.get(instance.getType());
        if (instancesForType != null) {
            instancesForType.remove(instance);

            if (instancesForType.size() == 0) {
                this.instancesByTypes.remove(instance.getType());
            }
        }

        if (!server.unloadWorld(world)) {
            throw new RuntimeException("Failed to unload instance world!"); // TODO Specialized exception
        }
    }

    public Optional<Instance> getInstance(String instanceName) {
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
    public void onClientConnection(ClientConnectionEvent event, @First Player player) {
        // We only care about these events
        if (!(event instanceof ClientConnectionEvent.Join || event instanceof ClientConnectionEvent.Disconnect)) {
            return;
        }

        final World world = player.getWorld();
        final Instance instance = getInstance(world.getName()).orElse(null);

        if (instance != null) {
            if (instance.getRegisteredPlayers().contains(player.getUniqueId())) {
                if (event instanceof ClientConnectionEvent.Join) {
                    // Player has played before, disconnected...let them come back as spectator
                    if (instance.getPlayerDeaths().containsKey(player.getUniqueId())) {
                        instance.convertPlayerToSpectator(player);
                    }
                } else {
                    // Player disconnecting instantly means they forfeit
                    instance.disqualifyPlayer(player, Cause.of(NamedCause.source(instance)));
                    if (instance.isRoundOver()) {
                        instance.advanceTo(Instance.State.PRE_END);
                    }
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onMoveEntity(MoveEntityEvent event) {

        if (!(event.getTargetEntity() instanceof Player)) {
            // We only care about Players.
            return;
        }

        final Player player = (Player) event.getTargetEntity();
        final World fromWorld = event.getFromTransform().getExtent();
        final World toWorld = event.getToTransform().getExtent();

        final Instance fromInstance = getInstance(fromWorld.getName()).orElse(null);
        final Instance toInstance = getInstance(toWorld.getName()).orElse(null);

        // We don't care about non-instances from and to.
        if (fromInstance == null && toInstance == null && !toWorld.getName().equals(Constants.Map.Lobby.DEFAULT_LOBBY_NAME)) {
            return;
        }

        // Movement inner-instance
        if (fromInstance != null && toInstance != null && fromInstance.equals(toInstance)) {
            // We only care about registered players
            if (fromInstance.getRegisteredPlayers().contains(player.getUniqueId())) {
                // If a Player has already spawned, this means they are playing. See if the instance allows movement
                if (fromInstance.getPlayerSpawns().containsKey(player.getUniqueId()) && !fromInstance.getState().canAnyoneMove()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Switching worlds
        if (!fromWorld.getUniqueId().equals(toWorld.getUniqueId())) {
            if (fromInstance != null) {
                // Switching out of instance means we kill them in the instance they left
                if (fromInstance.getRegisteredPlayers().contains(player.getUniqueId())) {
                    fromInstance.disqualifyPlayer(player, Cause.of(NamedCause.source(fromInstance)));
                    if (fromInstance.isRoundOver()) {
                        fromInstance.advanceTo(Instance.State.PRE_END);
                    }

                    // Set them back to the default scoreboard
                    player.setScoreboard(Sponge.getServer().getServerScoreboard().get());
                    player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).get());
                    player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
                    player.offer(Keys.CAN_FLY, true);
                }
            } else {
                // Switching into an instance
                if (toInstance != null && toInstance.getRegisteredPlayers().contains(player.getUniqueId())) {
                    // Already dead here? Adjust them as a spectator
                    if (toInstance.getPlayerDeaths().containsKey(player.getUniqueId())) {
                        toInstance.convertPlayerToSpectator(player);

                        player.setScoreboard(toInstance.getScoreboard().getHandle());
                    }
                } else if (toWorld.getName().equals(Constants.Map.Lobby.DEFAULT_LOBBY_NAME) && !player.hasPermission(Constants.Permissions.ADMIN)) {
                    // Going from a non-instance world to lobby
                    player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
                    player.offer(Keys.CAN_FLY, true);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onDestructEntity(DestructEntityEvent.Death event, @Getter("getTargetEntity") Player player) {
        final World world = player.getWorld();
        final Instance instance = getInstance(world.getName()).orElse(null);

        if (instance != null) {
            if (instance.getRegisteredPlayers().contains(player.getUniqueId())) {
                instance.disqualifyPlayer(player, event.getCause());
                if (instance.isRoundOver()) {
                    instance.advanceTo(Instance.State.PRE_END);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onRespawnPlayer(RespawnPlayerEvent event, @Getter("getTargetEntity") Player player) {
        final World fromWorld = event.getFromTransform().getExtent();
        World toWorld = event.getToTransform().getExtent();
        final Instance fromInstance = getInstance(fromWorld.getName()).orElse(null);
        Instance toInstance = getInstance(toWorld.getName()).orElse(null);

        if (fromInstance != null && toInstance == null) {
            if (fromInstance.getRegisteredPlayers().contains(player.getUniqueId())) {
                // Sometimes an instance is using a dimension that doesn't allow respawns. Override that logic so long as the instance world is
                // still loaded
                final Dimension dimension = fromWorld.getDimension();
                if (!dimension.allowsPlayerRespawns() && fromWorld.isLoaded()) {
                    event.setToTransform(event.getFromTransform());
                }
            }
        }

        toWorld = event.getToTransform().getExtent();
        toInstance = getInstance(toWorld.getName()).orElse(null);

        if (fromInstance != null) {
            if (toInstance != null && fromInstance.equals(toInstance)) {
                fromInstance.convertPlayerToSpectator(player);
            } else if (toInstance != null){
                player.setScoreboard(toInstance.getScoreboard().getHandle());
            } else {
                player.setScoreboard(Sponge.getServer().getServerScoreboard().get());
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onInteract(InteractEvent event, @Root Player player) {
        final World world = player.getWorld();
        final Instance instance = getInstance(world.getName()).orElse(null);

        if (instance != null && !instance.getState().canAnyoneInteract() && instance.getRegisteredPlayers().contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.LAST)
    public void onAttackEntity(AttackEntityEvent event) {
        final Entity victim = event.getTargetEntity();
        final World world = victim.getWorld();
        final Instance instance = getInstance(world.getName()).orElse(null);

        if (instance != null) {
            if (victim instanceof Player && !instance.getRegisteredPlayers().contains(victim.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onSignText(ChangeSignEvent event, @Root Player player) {
        if (this.isTpSign(event.getText().lines().get())) {
            if (player.hasPermission(Constants.Permissions.ADMIN)) {
                player.sendMessage(Text.of(TextColors.GREEN, "Sucessfully created world teleportation sign!"));
                event.getText().setElement(0, Text.of(TextColors.AQUA, event.getText().get(0).get()));
            } else {
                player.sendMessage(Text.of(TextColors.RED, "You don't have permission to create a world teleportation sign!"));
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onSignClick(InteractBlockEvent.Secondary.MainHand event, @Root Player player, @Getter("getTargetBlock") BlockSnapshot block) {
        block.getLocation().get().getTileEntity().flatMap(t -> t.get(Keys.SIGN_LINES)).ifPresent(lines -> {
            if (this.isTpSign(lines)) {
                String name = lines.get(1).toPlain();
                Optional<Instance> instance = Special.instance.getInstanceManager().getInstance(name);
                if (instance.isPresent()) {
                    if (!instance.get().canRegisterPlayer(player)) {
                        player.sendMessage(Text.of(TextColors.RED, "World is full!"));
                        return;
                    }
                    player.sendMessage(Text.of(TextColors.GREEN, "Joining world " + name));
                    instance.get().registerPlayer(player);
                    instance.get().spawnPlayer(player);
                } else {
                    player.sendMessage(Text.of(TextColors.RED, String.format("World %s isn't up yet!", name)));
                }
            }
        });
    }

    private boolean isTpSign(List<Text> lines) {
        return lines.get(0).toPlain().equalsIgnoreCase(Constants.Map.Lobby.SIGN_HEADER);
    }
}
