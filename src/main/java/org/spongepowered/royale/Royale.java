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
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterBuilderEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.RegisterRegistryEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;
import org.spongepowered.royale.configuration.MappedConfigurationAdapter;
import org.spongepowered.royale.instance.InstanceManager;
import org.spongepowered.royale.instance.InstanceType;
import org.spongepowered.royale.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.royale.instance.gen.InstanceMutator;
import org.spongepowered.royale.instance.gen.mutator.ChestMutator;
import org.spongepowered.royale.instance.gen.mutator.PlayerSpawnMutator;
import org.spongepowered.royale.template.ComponentTemplate;
import org.spongepowered.royale.template.ComponentTemplateTypeSerializer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Plugin(Constants.Plugin.ID)
public final class Royale {

    public static Royale instance;

    private final PluginContainer plugin;
    private final Path configFile;
    private final EventManager eventManager;
    private final Random random;
    private final ConfigurationOptions options;
    private final Logger logger;

    private InstanceManager instanceManager;

    @Inject
    public Royale(final PluginContainer plugin,
            @ConfigDir(sharedRoot = false) final Path configFile,
            final Game game,
            final ConfigManager configManager,
            final Logger logger) {
        Royale.instance = this;

        this.plugin = plugin;
        this.configFile = configFile;
        this.eventManager = game.eventManager();
        this.random = new Random();
        this.options = ConfigurationOptions.defaults()
                .serializers(configManager.serializers().childBuilder()
                            .register(ComponentTemplate.class, new ComponentTemplateTypeSerializer())
                                     .build());
        this.logger = logger;
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

    public ConfigurationOptions getConfigurationOptions() {
        return this.options;
    }

    @Listener
    public void onRegisterCatalogs(final RegisterRegistryEvent.EngineScoped<Server> event) {
        final Map<ResourceKey, InstanceMutator> defaultMutators = new HashMap<>();
        defaultMutators.put(ResourceKey.of(Constants.Plugin.ID, "chest"), new ChestMutator());
        defaultMutators.put(ResourceKey.of(Constants.Plugin.ID, "player_spawn"), new PlayerSpawnMutator());
        event.register(ResourceKey.of(Constants.Plugin.ID, "instance_mutator"), true, () -> defaultMutators);

        final Map<ResourceKey, InstanceType> defaultTypes = new HashMap<>();
        boolean createDefaults = true;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Constants.Map.INSTANCE_TYPES_FOLDER, entry -> entry.getFileName().toString()
                .endsWith(".conf"))) {
            for (final Path path : stream) {
                final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(
                        InstanceTypeConfiguration.class, this.options, path);

                try {
                    adapter.load();
                } catch (final IOException e) {
                    this.plugin.getLogger().error("Failed to load configuration for path [{}]!", path, e);
                    continue;
                }

                final String instanceId = path.getFileName().toString().split("\\.")[0].toLowerCase();
                final InstanceType newType = InstanceType.builder()
                        .key(ResourceKey.of(this.plugin, instanceId))
                        .from(adapter.getConfig())
                        .build();

                this.plugin.getLogger().info("Registered instance type '{}'", newType.key());
                defaultTypes.put(ResourceKey.of(this.plugin, instanceId), newType);
                createDefaults = false;
            }

        } catch (final IOException e) {
            throw new RuntimeException("Failed to iterate over the instance type configuration files!");
        }

        if (createDefaults) {
            final InstanceType defaultType = InstanceType.builder()
                    .key(ResourceKey.of(this.plugin, "last_man_standing"))
                    .name("Last Man Standing")
                    .build();
            defaultTypes.put(ResourceKey.of(this.plugin, "last_man_standing"), defaultType);
            this.plugin.getLogger().info("Registered instance type '{}'", defaultType.key());

            final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(
                    InstanceTypeConfiguration.class, this.options, Constants.Map.INSTANCE_TYPES_FOLDER.resolve(defaultType.key().value() +
                ".conf"));
            defaultType.injectIntoConfig(adapter.getConfig());
            try {
                adapter.save();
            } catch (final ConfigurateException e) {
                throw new RuntimeException(e);
            }
        }
        if (defaultTypes.isEmpty()) {
            throw new RuntimeException("Default InstanceTypes map is empty. Create defaults: " + createDefaults);
        }
        event.register(ResourceKey.of(Constants.Plugin.ID, "instance_type"), true, () -> defaultTypes);
    }

    @Listener
    public void onRegisterInstanceTypeBuilders(final RegisterBuilderEvent event) {
        event.register(InstanceType.Builder.class, InstanceType.Builder::new);
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        event.register(this.plugin, Commands.rootCommand(this.random), Constants.Plugin.ID, Constants.Plugin.ID.substring(0, 1));
    }

    //TODO
//    @Listener
//    public void onRegisterWorld(final RegisterWorldEvent event) {
//        event.register(Constants.Map.Lobby.LOBBY_WORLD_KEY, Constants.Map.Lobby.LOBBY_TEMPLATE);
//    }

    @Listener
    public void onStartingServer(final StartingEngineEvent<Server> event) {
        this.instanceManager = new InstanceManager(event.engine());
        this.eventManager.registerListeners(this.plugin, this.instanceManager);
    }

    @Listener
    public void onServerStarted(final StartedEngineEvent<Server> event) {
        final WorldManager worldManager = Sponge.server().worldManager();
        final Scheduler scheduler = Sponge.server().scheduler();
        worldManager.saveTemplate(Constants.Map.Lobby.LOBBY_TEMPLATE).handle((result, exception) -> {
            if (exception != null) {
                this.logger.fatal("UNABLE TO SAVE LOBBY WORLD TEMPLATE. Shutting down the server.", exception);
                scheduler.createExecutor(this.plugin).submit(() -> Sponge.server().shutdown());
            } else if (!result) {
                this.logger.fatal("UNABLE TO SAVE LOBBY WORLD TEMPLATE. Shutting down the server.");
                scheduler.createExecutor(this.plugin).submit(() -> Sponge.server().shutdown());
            } else {
                scheduler.createExecutor(this.plugin).submit(() -> {
                    worldManager.loadWorld(Constants.Map.Lobby.LOBBY_WORLD_KEY).handle((x, ex) -> {
                        if (ex != null) {
                            this.logger.fatal("UNABLE TO LOAD LOBBY WORLD. Shutting down the server.", e);
                            scheduler.createExecutor(this.plugin).submit(() -> Sponge.server().shutdown());
                        }
                        return null;
                    });
                    return null;
                });
            }
            return null;
        });
    }

    @Listener
    public void onStoppingServer(final StoppingEngineEvent<Server> event) {
        this.eventManager.unregisterListeners(this.instanceManager);
        this.instanceManager = null;
    }

    public InstanceManager getInstanceManager() {
        return this.instanceManager;
    }
}
