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
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterBuilderEvent;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.event.lifecycle.RegisterCatalogRegistryEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
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
import java.util.Random;

@Plugin(Constants.Plugin.ID)
public final class Royale {

    public static Royale instance;

    private final PluginContainer plugin;
    private final Path configFile;
    private final EventManager eventManager;
    private final InstanceManager instanceManager;
    private final Random random;
    private final ConfigurationOptions options;

    @Inject
    public Royale(final PluginContainer plugin, @ConfigDir(sharedRoot = false) final Path configFile, final Game game,
            final TypeSerializerCollection serializers) {
        Royale.instance = this;

        this.plugin = plugin;
        this.configFile = configFile;
        this.eventManager = game.getEventManager();
        this.instanceManager = new InstanceManager(game);
        this.random = new Random();
        this.options = ConfigurationOptions.defaults()
                .serializers(serializers.childBuilder()
                            .register(ComponentTemplate.class, new ComponentTemplateTypeSerializer())
                                     .build());
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
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Constants.Map.INSTANCE_TYPES_FOLDER, entry -> entry.getFileName().toString()
                .endsWith(".conf"))) {
            for (final Path path : stream) {
                final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(
                        InstanceTypeConfiguration.class, ConfigurationOptions.defaults(), path);

                try {
                    adapter.load();
                } catch (final IOException e) {
                    this.plugin.getLogger().error("Failed to load configuration for path [{}]!", path, e);
                    continue;
                }

                final String instanceId = path.getFileName().toString().split("\\.")[0].toLowerCase();
                final InstanceType newType = InstanceType.builder()
                        .from(adapter.getConfig())
                        .key(ResourceKey.of(this.plugin, instanceId))
                        .build();

                this.plugin.getLogger().info("Registered instance type '{}'", instanceId);
                event.register(newType);
            }

        } catch (final IOException e) {
            throw new RuntimeException("Failed to iterate over the instance type configuration files!");
        }
    }

    @Listener
    public void onRegisterInstanceTypeBuilders(final RegisterBuilderEvent event) {
        event.register(InstanceType.Builder.class, InstanceType.Builder::new);
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        event.register(this.plugin, Commands.rootCommand(this.random, this.instanceManager), Constants.Plugin.ID, Constants.Plugin.ID
                .substring(0, 1));
    }

    @Listener
    public void onStartedServer(final StartedEngineEvent<Server> event) {
        Sponge.getServer().getWorldManager().createProperties(Constants.Map.Lobby.LOBBY_WORLD_KEY, Constants.Map.Lobby.LOBBY_ARCHETYPE)
                .thenCompose(props -> Sponge.getServer().getWorldManager().loadWorld(props))
                .exceptionally(e -> {
                    this.plugin.getLogger().catching(e);
                    return null;
                });
    }
}
