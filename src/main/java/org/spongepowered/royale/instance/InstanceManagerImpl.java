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

import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.api.Instance;
import org.spongepowered.royale.api.InstanceManager;
import org.spongepowered.royale.instance.exception.InstanceAlreadyExistsException;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;
import org.spongepowered.royale.instance.gen.InstanceMutatorPipeline;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class InstanceManagerImpl implements InstanceManager {

    private final Map<ResourceKey, InstanceImpl> instances = new HashMap<>();

    @Override
    public CompletableFuture<Instance> createInstance(final ResourceKey key, final InstanceType type, final boolean force) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");

        return Sponge.server().worldManager().loadWorld(key).thenApplyAsync(w -> {
            if (w.border().diameter() >= 100000) {
                // Safety measure to prevent people from hanging the server
                throw new IllegalStateException("World border can't be bigger than 100k blocks");
            }
            w.properties().setSerializationBehavior(SerializationBehavior.AUTOMATIC_METADATA_ONLY);

            final InstanceImpl instance = new InstanceImpl(w, type);
            final InstanceImpl previous = this.instances.putIfAbsent(w.key(), instance);
            if (previous != null) {
                if (!force) {
                    throw new InstanceAlreadyExistsException(key.formatted());
                }
                if (previous.getState() != State.IDLE) {
                    throw new IllegalStateException("Instance is not IDLE");
                }
                this.instances.replace(w.key(), instance);
            }

            final InstanceMutatorPipeline pipeline = type.getMutatorPipeline();
            pipeline.mutate(instance);
            return instance;
        }, Royale.getInstance().getTaskExecutorService());
    }

    @Override
    public void startInstance(final ResourceKey key) throws UnknownInstanceException {
        Objects.requireNonNull(key, "key must not be null");
        final InstanceImpl instance = this.instances.get(key);
        if (instance == null) {
            throw new UnknownInstanceException(key.formatted());
        }

        instance.advanceTo(State.STARTING);
    }

    @Override
    public void endInstance(final ResourceKey key) throws UnknownInstanceException {
        Objects.requireNonNull(key, "key must not be null");
        final InstanceImpl instance = this.instances.get(key);
        if (instance == null) {
            throw new UnknownInstanceException(key.formatted());
        }

        instance.advanceTo(State.OVERTIME);
    }

    @Override
    public CompletableFuture<Boolean> unloadInstance(final ResourceKey key) {
        Objects.requireNonNull(key, "key must not be null");
        final InstanceImpl instance = this.instances.get(key);
        if (instance == null) {
            return CompletableFuture.completedFuture(true);
        }

        final ServerWorld world = instance.world();

        final ServerWorld lobby = Sponge.server().worldManager().world(Constants.Map.Lobby.LOBBY_WORLD_KEY)
                .orElse(Sponge.server().worldManager().defaultWorld());

        // Move everyone out
        for (ServerPlayer player : world.players()) {
            if (instance.isPlayerRegistered(player)) {
                instance.removePlayer(player);
            }
            player.sendMessage(Component.text("This instance is unloading. You are being moved to the lobby"));
            Sponge.server().serverScoreboard().ifPresent(player::setScoreboard);
            player.setLocation(ServerLocation.of(lobby, lobby.properties().spawnPosition()));
            player.offer(Keys.GAME_MODE, GameModes.SURVIVAL.get());
        }

        this.instances.remove(instance.getWorldKey());

        instance.updateSign();

        return Sponge.server().worldManager().unloadWorld(world);
    }

    @Override
    public Optional<Instance> getInstance(final ResourceKey key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(this.instances.get(key));
    }

    @Override
    public Collection<Instance> getAll() {
        return Collections.unmodifiableCollection(this.instances.values());
    }
}
