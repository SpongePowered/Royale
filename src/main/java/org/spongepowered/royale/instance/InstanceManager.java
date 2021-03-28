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

    private void convertToLobbyPlayer(final ServerPlayer player) {
        player.setScoreboard(this.server.serverScoreboard().orElse(null));
        Utils.resetPlayer(player);
    }

    private boolean isTeleportSign(final List<Component> lines) {
        return lines.size() != 0 && SpongeComponents.plainSerializer().serialize(lines.get(0)).equalsIgnoreCase(Constants.Map.Lobby.JOIN_SIGN_HEADER);
    }
}
