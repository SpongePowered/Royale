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
package org.spongepowered.special.map;

import org.apache.commons.io.FileUtils;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Constants;
import org.spongepowered.special.map.exception.InstanceAlreadyExistsException;
import org.spongepowered.special.map.exception.UnknownInstanceException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class MapManager {

    // World Name -> Instance
    private final Map<String, org.spongepowered.special.map.Map> instances = new HashMap<>();

    public void copyTemplate(MapType type) throws IOException {
        if (Sponge.getServer().getWorld(type.getTemplate()).isPresent()) {
            throw new IOException("Attempt to copy over template [" + type.getTemplate() + "] but a world with the same name is still loaded!");
        }

        if (this.instances.containsKey(type.getTemplate())) {
            throw new RuntimeException("Instance [" + type.getTemplate() + "] is still being managed and has now leaked!");
        }

        // Copy over template to worlds folder
        FileUtils.copyDirectory(type.getTemplatePath().toFile(), Sponge.getGame().getSavesDirectory().resolve(type.getTemplate()).toFile(), true);
    }

    public void createInstance(MapType type) throws IOException {
        if (Sponge.getServer().getWorld(type.getTemplate()).isPresent()) {
            throw new InstanceAlreadyExistsException(type.getTemplate());
        }

        if (this.instances.containsKey(type.getTemplate())) {
            throw new RuntimeException("Instance [" + type.getTemplate() + "] is still being managed and has now leaked!");
        }

        final World instance = Sponge.getServer().loadWorld(type.getTemplate()).orElseThrow(() -> new IOException("Failed to create instance for [" +
                type.getId() + "] using template [" + type.getTemplate() + "]."));

        this.instances.put(instance.getName(), new org.spongepowered.special.map.Map(instance.getName(), type, instance));
    }

    public void startInstance(String instanceName) throws UnknownInstanceException {
        final org.spongepowered.special.map.Map instance = this.instances.get(instanceName);
        if (instance == null) {
            throw new UnknownInstanceException(instanceName);
        }

        instance.start();
    }

    public void stopInstance(String instanceName) throws UnknownInstanceException {
        final org.spongepowered.special.map.Map instance = this.instances.get(instanceName);
        if (instance == null) {
            throw new UnknownInstanceException(instanceName);
        }

        instance.stop();
    }

    public Optional<org.spongepowered.special.map.Map> getInstance(String instanceName) {
        return Optional.ofNullable(this.instances.get(instanceName));
    }
    
    public CompletableFuture<Boolean> cleanupInstance(String instanceName) throws UnknownInstanceException {
        final Server server = Sponge.getServer();

        final World lobby = server.getWorld(Constants.Meta.ID + "_lobby").orElseThrow(() -> new RuntimeException("Lobby world was not "
                + "found!"));
        final World instance = server.getWorld(instanceName).orElseThrow(() -> new UnknownInstanceException(instanceName));

        // Move everyone out
        for (Player player : instance.getPlayers()) {
            player.setLocation(lobby.getSpawnLocation());
        }

        if (!server.unloadWorld(instance)) {
            throw new RuntimeException("Failed to unload instance world!"); // TODO Specialized exception
        }

        // Delete instance
        return server.deleteWorld(instance.getProperties());
    }

    @Listener(order = Order.LAST)
    public void onClientConnectionJoin(ClientConnectionEvent.Join event, @First Player player) {
        this.adjustGameModeFor(event.getTargetEntity().getWorld(), player);
    }

    @Listener(order = Order.LAST)
    public void onMoveEntityTeleport(MoveEntityEvent.Teleport event, @First Player player) {
        if (!event.getFromTransform().getExtent().getName().equalsIgnoreCase(event.getToTransform().getExtent().getName())) {
            this.adjustGameModeFor(event.getToTransform().getExtent(), player);
        }
    }

    private void adjustGameModeFor(World world, Player player) {
        final org.spongepowered.special.map.Map instance = this.instances.get(world.getName());
        if (instance != null) {
            // TODO adjust these from config
            player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
            player.offer(Keys.CAN_FLY, false);
        } else {
            if (world.getName().equalsIgnoreCase(Constants.Meta.ID + "_lobby")) {
                player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
                player.offer(Keys.CAN_FLY, true);
            }
        }
    }
}
