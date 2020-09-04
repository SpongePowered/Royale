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
package org.spongepowered.special;

import com.google.inject.Inject;
import net.kyori.adventure.text.TextComponent;
import org.apache.logging.log4j.Logger;
import org.slf4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.lifecycle.RegisterBuilderEvent;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.event.lifecycle.RegisterCatalogRegistryEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;
import org.spongepowered.special.instance.InstanceManager;
import org.spongepowered.special.instance.InstanceType;
import org.spongepowered.special.instance.InstanceTypeRegistryModule;
import org.spongepowered.special.instance.gen.InstanceMutator;
import org.spongepowered.special.instance.gen.InstanceMutatorRegistryModule;
import org.spongepowered.special.instance.gen.mutator.ChestMutator;
import org.spongepowered.special.instance.gen.mutator.PlayerSpawnMutator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Plugin(Constants.Meta.ID)
public final class Special {

    public static Special instance;
    private final InstanceManager instanceManager = new InstanceManager();
    private final Random random = new Random();
    @Inject private Logger logger;
    @Inject private PluginContainer container;
    @Inject @ConfigDir(sharedRoot = false) private Path configPath;

    public Special() {
        Special.instance = this;
        Sponge.getEventManager().registerListeners(this.container, this.instanceManager);
    }

    @Listener
    public void onGamePreinitialization(GamePreInitializationEvent event) {
        Sponge.getRegistry().registerModule(InstanceMutator.class, InstanceMutatorRegistryModule.getInstance());
        Sponge.getRegistry().registerModule(InstanceType.class, InstanceTypeRegistryModule.getInstance());
    }

    @Listener
    public void onRegisterCatalogs(RegisterCatalogRegistryEvent event) {
        event.register(InstanceMutator.class, Constants.key("instance_mutator"));
        event.register(InstanceType.class, Constants.key("instance_type"));
    }

    @Listener
    public void onRegisterMutators(RegisterCatalogEvent<InstanceMutator> event) {
        event.register(new ChestMutator());
        event.register(new PlayerSpawnMutator());
    }

    @Listener
    public void onRegisterInstanceTypes(RegisterCatalogEvent<InstanceType> event) {
        // TODO
    }

    @Listener
    public void onRegisterBuilder(final RegisterBuilderEvent event) {
        event.register(InstanceType.Builder.class, InstanceType.Builder::new);
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        event.register(this.container, Commands.rootCommand, Constants.Meta.ID, Constants.Meta.ID.substring(0, 1));
    }

    @Listener
    public void onGameStartingServer(final StartedEngineEvent<Server> event) {
        Sponge.getServer().getWorldManager().createProperties(Constants.Map.Lobby.DEFAULT_LOBBY_NAME, Constants.Map.Lobby.lobbyArchetype)
                .thenCompose(props -> Sponge.getServer().getWorldManager().loadWorld(props))
                .exceptionally(e -> {
                    this.logger.catching(e);
                    return null;
                });
    }

    @Listener
    public void onGameChat(PlayerChatEvent event, @Root Player player) {
        event.setCancelled(true);
        player.sendMessage(TextComponent.of("Chat has been disabled."));
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Path getConfigPath() {
        return this.configPath;
    }

    public InstanceManager getInstanceManager() {
        return this.instanceManager;
    }

    public Random getRandom() {
        return this.random;
    }
}
