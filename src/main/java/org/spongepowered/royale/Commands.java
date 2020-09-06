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

import static org.spongepowered.api.command.parameter.Parameter.*;

import com.google.common.collect.Iterables;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.royale.configuration.MappedConfigurationAdapter;
import org.spongepowered.royale.instance.Instance;
import org.spongepowered.royale.instance.InstanceType;
import org.spongepowered.royale.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

final class Commands {

    private static final Command.Parameterized createCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.create")
            .setShortDescription(TextComponent.of("Creates an instance."))
            .setExtendedDescription(TextComponent.of("Creates an instance from a ")
                    .append(format(NamedTextColor.GREEN, "world"))
                    .append(TextComponent.of(" with the specified instance "))
                    .append(format(NamedTextColor.LIGHT_PURPLE, "type"))
                    .append(TextComponent.of(".")))
            .parameter(catalogedElement(InstanceType.class).optional().setKey("instanceType").build())
            .parameter(worldProperties().optional().setKey("targetWorld").build())
            .setExecutor(context -> {
                InstanceType instanceType = context.<InstanceType>getOne("instanceType").orElse(null);
                WorldProperties targetProperties = context.<WorldProperties>getOne("targetWorld").orElse(null);

                if (instanceType == null) {
                    Collection<InstanceType> types = InstanceTypeRegistryModule.getInstance().getAll();
                    instanceType = Iterables.get(types, random.nextInt(types.size()));
                }

                if (targetProperties == null) {
                    Optional<WorldProperties> properties = Sponge.getServer().getWorldManager().getProperties(instanceType.getKey());
                    if (!properties.isPresent()) {
                        context.sendMessage(
                                format(NamedTextColor.RED, String.format("Unable to find a world using instance type id of %s",
                                        instanceType.getKey())));
                        return CommandResult.empty();
                    }
                    targetProperties = properties.get();
                }

                if (targetProperties.getKey().asString().length() > Constants.Map.MAXIMUM_WORLD_NAME_LENGTH) {
                    throw new CommandException(TextComponent.of(String
                            .format("World name %s is too long! It must be at most %s characters!", targetProperties.getKey(),
                                    Constants.Map.MAXIMUM_WORLD_NAME_LENGTH)));
                }

                context.sendMessage(LinearComponents.linear("Creating an instance from [", format(NamedTextColor.GREEN, targetProperties.getKey()), "] " 
                    + "using " 
                                + "instance ",
                        "type ", format(NamedTextColor.LIGHT_PURPLE, instanceType.getName()), "."));

                try {
                    Royale.instance.getInstanceManager().createInstance(targetProperties.getKey(), instanceType);
                } catch (Exception e) {
                    throw new CommandException(TextComponent.of(e.toString()), e);
                }

                context.sendMessage(LinearComponents.linear("Created instance for [", format(NamedTextColor.GREEN, targetProperties.getKey()), "]."));
                for (ServerPlayer player : Sponge.getServer().getOnlinePlayers()) {
                    if (player.getWorld().getKey().equals(Constants.Map.Lobby.DEFAULT_LOBBY_KEY)) {

                        final WorldProperties finalTargetProperties = targetProperties;
                        player.sendMessage(TextComponent.builder().clickEvent(ClickEvent.executeCallback(commandSource -> {
                            Optional<Instance> inst = Royale.instance.getInstanceManager().getInstance(finalTargetProperties.getKey());
                            if (inst.isPresent()) {
                                inst.get().registerPlayer((Player) commandSource);
                                inst.get().spawnPlayer((Player) commandSource);
                            }
                        })).append(LinearComponents.linear(TextComponent.of("["), format(NamedTextColor.RED, targetProperties.getKey().toString()),
                                TextComponent.of("] is ready! Right-click this message or the sign to join!"))).build());
                    }
                }
                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized registerCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.register")
            .setShortDescription(TextComponent.of("Registers an ")
                    .append(format(NamedTextColor.LIGHT_PURPLE, "instance type"))
                    .append(TextComponent.of(".")))
            .setExtendedDescription(TextComponent.of("Registers an ")
                    .append(format(NamedTextColor.LIGHT_PURPLE, "instance type"))
                    .append(TextComponent.of(" using a specified ID and/or name.")))
            .parameter(Parameter.string().setKey("id").build())
            .parameter(Parameter.string().setKey("name").optional().build())
            .setExecutor(context -> {
                final String id = context.<String>getOne("id").orElse(null);
                if (Sponge.getRegistry().getType(InstanceType.class, id).isPresent()) {
                    throw new CommandException(TextComponent.of("Unable to register [", format(NamedTextColor.LIGHT_PURPLE, id), "] as an instance with this ",
                            "name is already registered."));
                }

                final String name = context.<String>getOne("name").orElse(null);

                try {
                    InstanceTypeRegistryModule.getInstance().registerAdditionalCatalog(InstanceType.builder().build(id, name));
                    context.sendMessage(TextComponent.of("Registered instance type [", format(NamedTextColor.LIGHT_PURPLE, id), "]."));
                } catch (IOException | ObjectMappingException e) {
                    throw new CommandException(TextComponent.of("Failed to register instance [", format(NamedTextColor.LIGHT_PURPLE, id), "].", e));
                }
                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized startCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.start")
            .setShortDescription(TextComponent.of("Starts an ")
                    .append(format(NamedTextColor.LIGHT_PURPLE, "instance"))
                    .append(TextComponent.of(".")))
            .setExtendedDescription(TextComponent.of("Starts an ")
                    .append(format(NamedTextColor.LIGHT_PURPLE, "instance"))
                    .append(TextComponent.of(".")))
            .parameter(worldProperties().optional().setKey("targetWorld").build())
            .setExecutor(context -> {
                final Optional<WorldProperties> optWorldProperties = context.getOne("targetWorld");
                final ServerWorld world;
                if (optWorldProperties.isPresent()) {
                    world = optWorldProperties.get().getWorld().orElseThrow(() -> new CommandException(
                            TextComponent.of("World [").append(format(NamedTextColor.GREEN, optWorldProperties.get().getKey().toString()))
                                    .append(TextComponent.of("] is not online."))));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(TextComponent.of("World was not provided!"));
                }

                final Optional<Instance> optInstance = Royale.instance.getInstanceManager().getInstance(world.getKey());
                if (!optInstance.isPresent() || optInstance.get().getState().equals(Instance.State.IDLE)) {
                    try {
                        context.sendMessage(TextComponent.of("Starting round countdown in [", format(NamedTextColor.GREEN, world.getKey().toString()), "]"
                                + "."));
                        Royale.instance.getInstanceManager().startInstance(world.getKey());
                    } catch (UnknownInstanceException e) {
                        throw new CommandException(TextComponent.of("Unable to start round in [", format(NamedTextColor.GREEN, world.getKey().toString()),
                                "], was it created?"));
                    }
                } else {
                    context.sendMessage(TextComponent.of("Round already in progress."));
                }

                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized endCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.end")
            .setShortDescription(TextComponent.of("Ends an ", format(NamedTextColor.LIGHT_PURPLE, "instance"), "."))
            .setExtendedDescription(TextComponent.of("Ends an ", format(NamedTextColor.LIGHT_PURPLE, "instance"), "."))
            .parameter(worldProperties().optional().setKey("targetWorld").build())
            .parameter(bool().optional().setKey("force").build())
            .setExecutor(context -> {
                final Optional<WorldProperties> optWorldProperties = context.getOne("targetWorld");
                final ServerWorld world;
                if (optWorldProperties.isPresent()) {
                    Optional<ServerWorld> opt = optWorldProperties.get().getWorld();
                    if (!opt.isPresent() && Royale.instance.getInstanceManager().getInstance(optWorldProperties.get().getKey()).isPresent()) {
                        context.sendMessage(TextComponent.of(String.format("World %s was unloaded, but the instance still exists! Ending instance.",
                                                                optWorldProperties.get().getKey()),
                                NamedTextColor.YELLOW));
                        try {
                            Royale.instance.getInstanceManager().endInstance(optWorldProperties.get().getKey(), true);
                        } catch (UnknownInstanceException e) {
                            e.printStackTrace();
                        }
                        return CommandResult.empty();
                    }
                    world = opt.orElseThrow(() -> new CommandException(TextComponent.of(
                            "World [", format(NamedTextColor.GREEN, optWorldProperties.get().getKey().toString()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(TextComponent.of("World was not provided!"));
                }

                boolean force = context.<Boolean>getOne("force").orElse(false);

                try {
                    context.sendMessage(TextComponent.of((force ? "Forcibly e" : "E"), "nding round in [", format(NamedTextColor.GREEN, world.getKey()), "]."));
                    Royale.instance.getInstanceManager().endInstance(world.getKey(), force);
                } catch (UnknownInstanceException e) {
                    throw new CommandException(TextComponent.of("Unable to end round in [", format(NamedTextColor.GREEN, world.getKey()), "]!"));
                }

                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized joinCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.join")
            .setShortDescription(TextComponent.of("Joins an ", format(NamedTextColor.LIGHT_PURPLE, "instance"), "."))
            .setExtendedDescription(TextComponent.of("Joins an ", format(NamedTextColor.LIGHT_PURPLE, "instance"), "."))
            .arguments(optional(world(TextComponent.of("targetWorld"))), optional(player(TextComponent.of("player"))))
            .setExecutor(context -> {
                final Optional<WorldProperties> optWorldProperties = context.getOne("targetWorld");
                final ServerWorld world;
                if (optWorldProperties.isPresent()) {
                    world = optWorldProperties.get().getWorld().orElseThrow(() -> new CommandException(TextComponent.of(
                            "World [", format(NamedTextColor.GREEN, optWorldProperties.get().getKey()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(TextComponent.of("World was not provided!"));
                }

                final Optional<ServerPlayer> optPlayer = context.getOne("player");
                final ServerPlayer player;
                if (optPlayer.isPresent()) {
                    player = optPlayer.get();
                } else if (src instanceof ServerPlayer) {
                    player = (Player) src;
                } else {
                    throw new CommandException(TextComponent.of("Player was not specified and source was not a player!"));
                }

                final Optional<Instance> instance = Royale.instance.getInstanceManager().getInstance(world.getKey());
                if (!instance.isPresent()) {
                    throw new CommandException(TextComponent.of("Instance [", format(NamedTextColor.GREEN, world.getKey()), "] is not a valid instance, is it ",
                            "running?"));
                }

                player.sendMessage(TextComponent.of("Joining [", format(NamedTextColor.GREEN, world.getKey()), "]."));
                instance.get().registerPlayer(player);
                instance.get().spawnPlayer(player);

                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized reloadCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.reload")
            .setShortDescription(TextComponent.of("Reloads the configuration of an ", format(NamedTextColor.LIGHT_PURPLE, "instance type"), "."))
            .setExtendedDescription(TextComponent.of("Reloads the configuration an ", format(NamedTextColor.LIGHT_PURPLE, "instance type"), "."))
            .arguments(catalogedElement(TextComponent.of("instanceType"), InstanceType.class))
            .setExecutor(context -> {
                final InstanceType instanceType = context.<InstanceType>getOne("instanceType").orElseThrow(() -> new CommandException(TextComponent.of
                        ("Specified instance is not valid!")));

                final Path configPath = Constants.Map.INSTANCE_TYPES_FOLDER.resolve(instanceType.getId() + ".conf");
                final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(
                        InstanceTypeConfiguration.class, Constants.Map.DEFAULT_OPTIONS, configPath);

                try {
                    adapter.load();
                } catch (IOException | ObjectMappingException e) {
                    throw new CommandException(
                            TextComponent.of("Unable to load configuration for instance type [", format(NamedTextColor.LIGHT_PURPLE, instanceType
                                    .getId()), "]."));
                }

                instanceType.injectFromConfig(adapter.getConfig());

                context.sendMessage(TextComponent.of("Reloaded configuration for instance type [", format(NamedTextColor.LIGHT_PURPLE, instanceType.getId()), "]."));

                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized setSerializationCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.set.serialization")
            .setShortDescription(TextComponent.of("Sets the serialization property of an ", format(NamedTextColor.LIGHT_PURPLE, "instance"), "."))
            .arguments(optional(world(TextComponent.of("targetWorld"))), catalogedElement(TextComponent.of("serializationBehavior"), SerializationBehavior.class))
            .setExecutor(context -> {
                final Optional<WorldProperties> optWorldProperties = context.getOne("targetWorld");
                final ServerWorld world;
                if (optWorldProperties.isPresent()) {
                    world = optWorldProperties.get().getWorld().orElseThrow(() -> new CommandException(TextComponent.of(
                            "World [", format(NamedTextColor.GREEN, optWorldProperties.get().getKey()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(TextComponent.of("World was not provided!"));
                }

                final SerializationBehavior serializationBehavior = context.<SerializationBehavior>getOne("serializationBehavior").orElseThrow(() ->
                        new CommandException(TextComponent.of("Invalid serialization behavior!")));

                world.setSerializationBehavior(serializationBehavior);
                context.sendMessage(TextComponent.of("World [", format(NamedTextColor.GREEN, world.getKey()), "] set to serialization behavior [", format
                        (NamedTextColor.YELLOW, serializationBehavior.getName()), "]."));

                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized setCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.set")
            .setShortDescription(TextComponent.of("Sets a property of an ", format(NamedTextColor.LIGHT_PURPLE, "instance"), "."))
            .child(setSerializationCommand, "serialization")
            .build();

    private static final Command.Parameterized tpWorldCommand = Command.builder()
            .setShortDescription(TextComponent.of("Teleports a player to another ", format(NamedTextColor.GREEN, "world"), "."))
            .arguments(seq(playerOrSource(TextComponent.of("targetPlayer")), onlyOne(world(TextComponent.of("targetWorld")))))
            .setPermission(Constants.Meta.ID + ".command.tpworld")
            .setExecutor(context -> {
                final Optional<WorldProperties> optWorldProperties = context.getOne("targetWorld");
                final ServerWorld world;
                if (optWorldProperties.isPresent()) {
                    world = optWorldProperties.get().getWorld().orElseThrow(() -> new CommandException(TextComponent.of(
                            "World [", format(NamedTextColor.GREEN, optWorldProperties.get().getKey()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(TextComponent.of("World was not provided!"));
                }
                for (Player target : context.<Player>getAll("targetPlayer")) {
                    target.setLocation(new Location<>(world, world.getProperties().getSpawnPosition()));
                }
                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized worldModifiedCommand = Command.builder()
            .setShortDescription(TextComponent.of("Sets whether a world has been modified"))
            .setExtendedDescription(TextComponent.of("This controls whether or not a fast mutator pass can be used"))
            .arguments(world(TextComponent.of("world")), optional(bool(TextComponent.of("modified")), true))
            .setPermission(Constants.Permissions.WORLD_MODIFIED_COMMAND)
            .setExecutor(context -> {
                WorldProperties properties = context.<WorldProperties>getOne("world").orElse(null);
                boolean modified = context.<Boolean>getOne("modified").orElse(null);
                Royale.instance.getInstanceManager().setWorldModified(properties.getKey(), modified);

                context.sendMessage(
                        TextComponent.of(NamedTextColor.GREEN, String.format("Set modified state of world %s to %s!", properties.getKey(), modified)));
                return CommandResult.success();
            })
            .build();

    private static final Command.Parameterized loadWorldCommand = Command.builder()
            .setShortDescription(TextComponent.of("Manually loads a world"))
            .arguments(world(TextComponent.of("world")))
            .setPermission(Constants.Permissions.WORLD_LOAD_COMMAND)
            .setExecutor(context -> {
                WorldProperties properties = context.<WorldProperties>getOne("world").orElse(null);

                if (Sponge.getServer().getWorld(properties.getUniqueId()).isPresent()) {
                    context.sendMessage(TextComponent.of(NamedTextColor.YELLOW, String.format("World %s is already loaded!", properties.getKey())));
                    return CommandResult.empty();
                }

                Optional<World> world = Sponge.getServer().loadWorld(properties);
                if (world.isPresent()) {
                    context.sendMessage(TextComponent.of(NamedTextColor.GREEN, String.format("Successfully loaded world %s", properties.getKey())));
                    return CommandResult.success();
                } else {
                    context.sendMessage(TextComponent.of(NamedTextColor.RED, String.format("Unable to load world %s", properties.getKey())));
                    return CommandResult.empty();
                }
            })
            .build();

    private static final Command.Parameterized unloadWorldCommand = Command.builder()
            .setShortDescription(TextComponent.of("Manually unloads a world"))
            .arguments(world(TextComponent.of("world")))
            .setPermission(Constants.Permissions.WORLD_UNLOAD_COMMAND)
            .setExecutor(context -> {
                WorldProperties properties = context.<WorldProperties>getOne("world").orElse(null);

                Optional<World> world = Sponge.getServer().getWorld(properties.getUniqueId());
                if (!world.isPresent()) {
                    context.sendMessage(TextComponent.of(String.format("World %s is not loaded!", properties.getKey())));
                    return CommandResult.empty();
                }

                if (Royale.instance.getInstanceManager().getInstance(properties.getKey()).isPresent()) {
                    context.sendMessage(TextComponent.of(NamedTextColor.RED,
                            String.format("Instance %s is currently running! Use '/s end %s' to end it!", properties.getKey(),
                                    properties.getKey())));
                    return CommandResult.empty();
                }

                if (Sponge.getServer().unloadWorld(world.get())) {
                    context.sendMessage(TextComponent.of(NamedTextColor.GREEN, String.format("Successfully unloaded world %s", properties.getKey())));
                    return CommandResult.success();
                } else {
                    context.sendMessage(TextComponent.of(NamedTextColor.RED, String.format("Unable to unload world %s", properties.getKey())));
                    return CommandResult.empty();
                }
            })
            .build();

    static final Command.Parameterized rootCommand = Command.builder()
            .setPermission(Constants.Meta.ID + ".command.help")
            .setShortDescription(TextComponent.of("Displays available commands"))
            .setExtendedDescription(TextComponent.of("Displays available commands")) // TODO Do this better
            .setExecutor(context -> {
                context.sendMessage(TextComponent.of("Some help should go here..."));
                return CommandResult.success();
            })
            .child(createCommand, "create", "c")
            .child(registerCommand, "register", "reg")
            .child(startCommand, "start")
            .child(endCommand, "end", "e")
            .child(joinCommand, "join", "j")
            .child(reloadCommand, "reload", "rel")
            .child(setCommand, "set")
            .child(tpWorldCommand, "tpworld", "tpw")
            .child(worldModifiedCommand, "worldmodified", "modified", "wm")
            .child(loadWorldCommand, "loadworld", "load", "lw")
            .child(unloadWorldCommand, "unloadworld", "unload", "uw")
            .build();

    private static TextComponent format(NamedTextColor color, String content) {
        return TextComponent.of(content, color);
    }
}
