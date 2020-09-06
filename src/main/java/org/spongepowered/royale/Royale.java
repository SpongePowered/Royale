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
package org.spongepowered.royale;

import com.google.inject.Inject;
import net.kyori.adventure.text.TextComponent;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterBuilderEvent;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.event.lifecycle.RegisterCatalogRegistryEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;
import org.spongepowered.royale.instance.InstanceManager;
import org.spongepowered.royale.instance.InstanceType;
import org.spongepowered.royale.instance.gen.InstanceMutator;
import org.spongepowered.royale.instance.gen.mutator.ChestMutator;
import org.spongepowered.royale.instance.gen.mutator.PlayerSpawnMutator;

import java.nio.file.Path;
import java.util.Random;

@Plugin(Constants.Plugin.ID)
public final class Royale {

    public static Royale instance;

    private final PluginContainer plugin;
    private final Path configFile;
    private final EventManager eventManager;
    private final InstanceManager instanceManager;
    private final Random random;

    @Inject
    public Royale(final PluginContainer plugin, @ConfigDir(sharedRoot = false) final Path configFile, final EventManager eventManager,
            final InstanceManager instanceManager) {
        Royale.instance = this;
        this.plugin = plugin;
        this.configFile = configFile;
        this.eventManager = eventManager;
        this.instanceManager = instanceManager;
        this.random = new Random();
    }

    public PluginContainer getPlugin() {
        return this.plugin;
    }

    public Path getConfigFile() {
        return this.configFile;
    }

    public Random getRandom() {
        return this.random;
    }

    @Listener
    public void onConstructPlugin(final ConstructPluginEvent event) {
        this.eventManager.registerListeners(this.plugin, this.instanceManager);
    }

    @Listener
    public void onRegisterCatalogs(final RegisterCatalogRegistryEvent event) {
        event.register(InstanceMutator.class, ResourceKey.of(this.plugin, "instance_mutator"));
        event.register(InstanceType.class, ResourceKey.of(this.plugin, "instance_type"));
    }

    @Listener
    public void onRegisterMutators(final RegisterCatalogEvent<InstanceMutator> event) {
        event.register(new ChestMutator());
        event.register(new PlayerSpawnMutator());
    }

    @Listener
    public void onRegisterInstanceTypes(final RegisterCatalogEvent<InstanceType> event) {
        // TODO
    }

    @Listener
    public void onRegisterBuilder(final RegisterBuilderEvent event) {
        event.register(InstanceType.Builder.class, InstanceType.Builder::new);
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        event.register(this.plugin, Commands.rootCommand, Constants.Plugin.ID, Constants.Plugin.ID.substring(0, 1));
    }

    @Listener
    public void onGameStartingServer(final StartedEngineEvent<Server> event) {
        Sponge.getServer().getWorldManager().createProperties(Constants.Map.Lobby.DEFAULT_LOBBY_KEY, Constants.Map.Lobby.LOBBY_ARCHETYPE)
                .thenCompose(props -> Sponge.getServer().getWorldManager().loadWorld(props))
                .exceptionally(e -> {
                    this.plugin.getLogger().catching(e);
                    return null;
                });
    }

    @Listener
    public void onGameChat(final PlayerChatEvent event, @Root final Player player) {
        event.setCancelled(true);
        player.sendMessage(TextComponent.of("Chat has been disabled."));
    }
}
