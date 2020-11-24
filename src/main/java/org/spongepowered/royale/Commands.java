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

import com.google.common.collect.Iterables;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommonParameters;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.CatalogedValueParameters;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.royale.configuration.MappedConfigurationAdapter;
import org.spongepowered.royale.instance.Instance;
import org.spongepowered.royale.instance.InstanceType;
import org.spongepowered.royale.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

final class Commands {

    private static final Parameter.Value<InstanceType> INSTANCE_TYPE_PARAMETER_OPTIONAL =
            Parameter.catalogedElement(InstanceType.class).optional().setKey("instanceType").build();
    private static final Parameter.Value<InstanceType> INSTANCE_TYPE_PARAMETER =
            Parameter.catalogedElement(InstanceType.class).setKey("instanceType").build();
    private static final Parameter.Value<Boolean> FORCE_PARAMETER = Parameter.bool().setKey("force").orDefault(false).build();
    private static final Parameter.Value<Boolean> MODIFIED_PARAMETER = Parameter.bool().setKey("modified").orDefault(true).build();
    private static final Parameter.Value<SerializationBehavior> SERIALIZATION_BEHAVIOR_PARAMETER =
            Parameter.enumValue(SerializationBehavior.class).setKey("behavior").build();
    private static final Parameter.Value<List<ServerPlayer>> MANY_PLAYERS =
            Parameter.builder(new TypeToken<List<ServerPlayer>>() {}).parser(CatalogedValueParameters.MANY_PLAYERS).orDefault((CommandCause cause) -> {
                if (cause.root() instanceof ServerPlayer) {
                    return Collections.singletonList((ServerPlayer) cause.root());
                }
                return Collections.emptyList();
            }).setKey("players").build();

    private static ServerWorld getWorld(final CommandContext context) throws CommandException {
        final Optional<WorldProperties> optWorldProperties = context.getOne(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY_OPTIONAL);
        if (optWorldProperties.isPresent()) {
            return optWorldProperties.get().getWorld().orElseThrow(() -> new CommandException(
                    Component.text("World [").append(format(NamedTextColor.GREEN, optWorldProperties.get().getKey().toString()))
                            .append(Component.text("] is not online."))));
        } else if (context.getCause().getLocation().isPresent()) {
            return context.getCause().getLocation().get().getWorld();
        } else {
            throw new CommandException(Component.text("World was not provided!"));
        }
    }

    private static Command.Parameterized createCommand(final Random random) {
        return Command.builder()
                .setPermission(Constants.Plugin.ID + ".command.create")
                .setShortDescription(Component.text("Creates an instance."))
                .setExtendedDescription(Component.text("Creates an instance from a ")
                        .append(format(NamedTextColor.GREEN, "world"))
                        .append(Component.text(" with the specified instance "))
                        .append(format(NamedTextColor.LIGHT_PURPLE, "type"))
                        .append(Component.text(".")))
                .parameter(Commands.INSTANCE_TYPE_PARAMETER_OPTIONAL)
                .parameter(CommonParameters.ALL_WORLD_PROPERTIES)
                .setExecutor(context -> {
                    final InstanceType instanceType = context.getOne(Commands.INSTANCE_TYPE_PARAMETER_OPTIONAL).orElseGet(() -> {
                        final Collection<InstanceType> types = Sponge.getRegistry().getCatalogRegistry().getAllOf(InstanceType.class);
                        return Iterables.get(types, random.nextInt(types.size()));
                    });
                    final WorldProperties targetProperties = context.getOne(CommonParameters.ALL_WORLD_PROPERTIES).orElseGet(() -> {
                        final Optional<WorldProperties> properties = Sponge.getServer().getWorldManager().getProperties(instanceType.getKey());
                        return properties.orElse(null);
                    });

                    if (targetProperties == null) {
                        throw new CommandException(
                                Commands.format(NamedTextColor.RED, String.format("Unable to find a world using instance type id of %s",
                                        instanceType.getKey())));
                    }

                    context.sendMessage(Identity.nil(),
                            Component.text().content("Creating an instance from [")
                                    .append(Commands.format(NamedTextColor.GREEN, targetProperties.getKey().asString()))
                                    .append(Component.text("] using instance type "))
                                    .append(Commands.format(NamedTextColor.LIGHT_PURPLE, instanceType.getName()))
                                    .append(Component.text("."))
                                    .build()
                    );

                    try {
                        Royale.instance.getInstanceManager().createInstance(targetProperties.getKey(), instanceType);
                    } catch (final Exception e) {
                        throw new CommandException(Component.text(e.toString()), e);
                    }

                    context.sendMessage(Identity.nil(),
                            Component.text().content("Created instance for [")
                                    .append(Commands.format(NamedTextColor.GREEN, targetProperties.getKey().asString()))
                                    .append(Component.text("]"))
                                    .build()
                    );

                    for (final ServerPlayer player : Sponge.getServer().getOnlinePlayers()) {
                        if (player.getWorld().getKey().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                            player.sendMessage(Identity.nil(), Component.text().clickEvent(SpongeComponents.executeCallback(commandCause -> {
                                final Optional<Instance> inst = Royale.instance.getInstanceManager().getInstance(targetProperties.getKey());
                                if (inst.isPresent()) {
                                    final ServerPlayer serverPlayer = (ServerPlayer) commandCause.root();
                                    if (inst.get().registerPlayer(serverPlayer)) {
                                        inst.get().spawnPlayer(serverPlayer);
                                    }
                                }
                            })).append(LinearComponents
                                    .linear(Component.text("["), format(NamedTextColor.RED, targetProperties.getKey().toString()),
                                            Component.text("] is ready! Right-click this message or the sign to join!"))).build());
                        }
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized startCommand() {
        return Command.builder()
                .setPermission(Constants.Plugin.ID + ".command.start")
                .setShortDescription(Component.text("Starts an ")
                        .append(format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text(".")))
                .setExtendedDescription(Component.text("Starts an ")
                        .append(format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text(".")))
                .parameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY_OPTIONAL)
                .setExecutor(context -> {
                    final ServerWorld world = Commands.getWorld(context);
                    final Optional<Instance> optInstance = Royale.instance.getInstanceManager().getInstance(world.getKey());
                    if (!optInstance.isPresent() || optInstance.get().getState().equals(Instance.State.IDLE)) {
                        try {
                            context.sendMessage(Identity.nil(),
                                    Component.text().content("Starting round countdown in [")
                                        .append(Commands.format(NamedTextColor.GREEN, world.getKey().toString()))
                                        .append(Component.text("]."))
                                        .build());
                            Royale.instance.getInstanceManager().startInstance(world.getKey());
                        } catch (final UnknownInstanceException e) {
                            throw new CommandException(Component.text().content("Unable to start round in [")
                                        .append(Commands.format(NamedTextColor.GREEN, world.getKey().toString()))
                                        .append(Component.text("], was it created?"))
                                        .build()
                            );
                        }
                    } else {
                        context.sendMessage(Identity.nil(), Component.text("Round already in progress."));
                    }

                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized endCommand() {
        return Command.builder()
                .setPermission(Constants.Plugin.ID + ".command.end")
                .setShortDescription(Component.text().content("Ends an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text("."))
                        .build())
                .parameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY_OPTIONAL)
                .parameter(Commands.FORCE_PARAMETER)
                .setExecutor(context -> {
                    final Optional<WorldProperties> optWorldProperties = context.getOne(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY_OPTIONAL);
                    final ServerWorld world;
                    if (optWorldProperties.isPresent()) {
                        final Optional<ServerWorld> opt = optWorldProperties.get().getWorld();
                        if (!opt.isPresent() && Royale.instance.getInstanceManager().getInstance(optWorldProperties.get().getKey()).isPresent()) {
                            context.sendMessage(Identity.nil(),
                                    Component.text(String.format("World %s was unloaded, but the instance still exists! Ending instance.",
                                            optWorldProperties.get().getKey()),
                                            NamedTextColor.YELLOW));
                            try {
                                Royale.instance.getInstanceManager().endInstance(optWorldProperties.get().getKey(), true);
                            } catch (final UnknownInstanceException e) {
                                e.printStackTrace();
                            }
                            return CommandResult.empty();
                        }
                        world = opt.orElseThrow(() -> new CommandException(Component.text().content("World [")
                                .append(Commands.format(NamedTextColor.GREEN, optWorldProperties.get().getKey().toString()))
                                .append(Component.text("] is not online."))
                                .build()));
                    } else {
                        world = context.getCause().getLocation().map(Location::getWorld).orElseThrow(() ->
                                new CommandException(Component.text("World was not provided!")));
                    }

                    final boolean force = context.requireOne(Commands.FORCE_PARAMETER);

                    try {
                        context.sendMessage(Identity.nil(), Component.text()
                                .content(force ? "Forcibly e" : "E")
                                .append(Component.text("nding round in ["))
                                .append(Commands.format(NamedTextColor.GREEN, world.getKey().asString()))
                                .append(Component.text("]."))
                                .build());
                        Royale.instance.getInstanceManager().endInstance(world.getKey(), force);
                    } catch (final UnknownInstanceException e) {
                        throw new CommandException(
                                Component.text().content("Unable to end round in [")
                                    .append(Commands.format(NamedTextColor.GREEN, world.getKey().asString()))
                                    .append(Component.text("]!"))
                                    .build());
                    }

                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized joinCommand() {
        return Command.builder()
                .setPermission(Constants.Plugin.ID + ".command.join")
                .setShortDescription(Component.text()
                        .content("Joins an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text("."))
                        .build())
                .parameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY_OPTIONAL)
                .parameter(CommonParameters.PLAYER_OR_SOURCE)
                .setExecutor(context -> {
                    final ServerWorld world = Commands.getWorld(context);

                    final ServerPlayer player = context.requireOne(CommonParameters.PLAYER_OR_SOURCE);
                    final Optional<Instance> instance = Royale.instance.getInstanceManager().getInstance(world.getKey());
                    if (!instance.isPresent()) {
                        throw new CommandException(
                                Component.text().content("Instance [")
                                    .append(Commands.format(NamedTextColor.GREEN, world.getKey().asString()))
                                    .append(Component.text("] is not a valid instance, is it running?"))
                                    .build());
                    }

                    player.sendMessage(Identity.nil(), Component.text().content("Joining [")
                            .append(Commands.format(NamedTextColor.GREEN, world.getKey().asString()))
                            .append(Component.text("]."))
                            .build());
                    if (instance.get().registerPlayer(player)) {
                        instance.get().spawnPlayer(player);
                    }

                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized reloadCommand()  {
        return Command.builder()
                .setPermission(Constants.Plugin.ID + ".command.reload")
                .setShortDescription(Component.text()
                        .content("Reloads the configuration of an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance type"))
                        .append(Component.text("."))
                        .build())
                .parameter(Commands.INSTANCE_TYPE_PARAMETER)
                .setExecutor(context -> {
                    final InstanceType instanceType = context.requireOne(Commands.INSTANCE_TYPE_PARAMETER);
                    final Path configPath = Constants.Map.INSTANCE_TYPES_FOLDER.resolve(instanceType.getKey().getValue() + ".conf");
                    final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(
                            InstanceTypeConfiguration.class, Royale.instance.getConfigurationOptions(), configPath, false);

                    try {
                        adapter.load();
                    } catch (final ConfigurateException e) {
                        throw new CommandException(
                                Component.text().content("Unable to load configuration for instance type [")
                                    .append(Commands.format(NamedTextColor.LIGHT_PURPLE, instanceType.getKey().asString()))
                                    .append(Component.text("]."))
                                    .build());
                    }

                    instanceType.injectFromConfig(adapter.getConfig());

                    context.sendMessage(Identity.nil(), Component.text().content("Reloaded configuration for instance type [")
                            .append(Commands.format(NamedTextColor.LIGHT_PURPLE, instanceType.getKey().asString()))
                            .append(Component.text("]."))
                            .build());

                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized setSerializationCommand() {
        return Command.builder()
                .setPermission(Constants.Plugin.ID + ".command.set.serialization")
                .setShortDescription(Component.text().content("Sets the serialization property of an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text("."))
                        .build())
                .parameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY_OPTIONAL)
                .parameter(Commands.SERIALIZATION_BEHAVIOR_PARAMETER)
                .setExecutor(context -> {
                    final ServerWorld world = Commands.getWorld(context);
                    final SerializationBehavior serializationBehavior = context.requireOne(Commands.SERIALIZATION_BEHAVIOR_PARAMETER);

                    world.getProperties().setSerializationBehavior(serializationBehavior);
                    context.sendMessage(Identity.nil(), Component.text().content("World [")
                            .append(Commands.format(NamedTextColor.GREEN, world.getKey().asString()))
                            .append(Component.text("] set to serialization behavior ["))
                            .append(Commands.format(NamedTextColor.YELLOW, serializationBehavior.name().toLowerCase()))
                            .append(Component.text("]."))
                            .build());

                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized setCommand() {
        return Command.builder()
                .setPermission(Constants.Plugin.ID + ".command.set")
                .setShortDescription(Component.text().content("Sets a property of an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text("."))
                        .build())
                .child(Commands.setSerializationCommand(), "serialization")
                .build();
    }

    private static Command.Parameterized tpWorldCommand() {
        return Command.builder()
                .setShortDescription(Component.text().content("Teleports a player to another ")
                        .append(Commands.format(NamedTextColor.GREEN, "world"))
                        .append(Component.text("."))
                        .build())
                .parameter(Commands.MANY_PLAYERS)
                .parameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY_OPTIONAL)
                .setPermission(Constants.Plugin.ID + ".command.tpworld")
                .setExecutor(context -> {
                    final ServerWorld world = Commands.getWorld(context);
                    for (final ServerPlayer target : context.requireOne(Commands.MANY_PLAYERS)) {
                        target.setLocation(ServerLocation.of(world, world.getProperties().getSpawnPosition()));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized worldModifiedCommand() {
        return Command.builder()
                .setShortDescription(Component.text("Sets whether a world has been modified"))
                .setExtendedDescription(Component.text("This controls whether or not a fast mutator pass can be used"))
                .parameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY)
                .parameter(Commands.MODIFIED_PARAMETER)
                .setPermission(Constants.Permissions.WORLD_MODIFIED_COMMAND)
                .setExecutor(context -> {
                    final WorldProperties properties = context.requireOne(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY);
                    final boolean modified = context.requireOne(Commands.MODIFIED_PARAMETER);
                    Royale.instance.getInstanceManager().setWorldModified(properties.getKey(), modified);

                    context.sendMessage(Identity.nil(),
                            Commands.format(NamedTextColor.GREEN, String.format("Set modified state of world %s to %s!", properties.getKey(), modified)));
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized loadWorldCommand() {
        return Command.builder()
                .setShortDescription(Component.text("Manually loads a world"))
                .parameter(CommonParameters.ALL_WORLD_PROPERTIES)
                .setPermission(Constants.Permissions.WORLD_LOAD_COMMAND)
                .setExecutor(context -> {
                    final WorldProperties properties = context.requireOne(CommonParameters.ALL_WORLD_PROPERTIES);
                    if (properties.getWorld().isPresent()) {
                        throw new CommandException(Commands.format(NamedTextColor.YELLOW, String.format("World %s is already loaded!", properties.getKey())));
                    }

                    context.sendMessage(Identity.nil(),
                            Commands.format(NamedTextColor.GREEN, String.format("Loading world %s...", properties.getKey())));
                    final CompletableFuture<ServerWorld> future = Sponge.getServer().getWorldManager().loadWorld(properties);
                    future.whenComplete((world, throwable) -> {
                        if (throwable != null) {
                            context.sendMessage(Identity.nil(),
                                    Commands.format(NamedTextColor.RED, String.format("Unable to load world %s", properties.getKey())));
                        } else {
                            context.sendMessage(Identity.nil(),
                                    Commands.format(NamedTextColor.GREEN, String.format("Successfully loaded world %s", properties.getKey())));
                        }
                    });

                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized unloadWorldCommand() {
        return Command.builder()
                .setShortDescription(Component.text("Manually unloads a world"))
                .parameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY)
                .setPermission(Constants.Permissions.WORLD_UNLOAD_COMMAND)
                .setExecutor(context -> {
                    final WorldProperties properties = context.requireOne(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY);

                    if (!properties.getWorld().isPresent()) {
                        throw new CommandException(Component.text(String.format("World %s is not loaded!", properties.getKey())));
                    }

                    if (Royale.instance.getInstanceManager().getInstance(properties.getKey()).isPresent()) {
                        throw new CommandException(Commands.format(NamedTextColor.RED,
                                String.format("Instance %s is currently running! Use '/s end %s' to end it!", properties.getKey(),
                                        properties.getKey())));
                    }

                    context.sendMessage(Identity.nil(),
                            Commands.format(NamedTextColor.GREEN, String.format("Unloading world %s...", properties.getKey())));
                    final CompletableFuture<Boolean> unloadFuture = Sponge.getServer().getWorldManager().unloadWorld(properties.getKey());
                    unloadFuture.whenComplete((result, throwable) -> {
                        if (throwable != null || !result) {
                            context.sendMessage(Identity.nil(),
                                    Commands.format(NamedTextColor.RED, String.format("Unable to unload world %s", properties.getKey())));
                        } else {
                            context.sendMessage(Identity.nil(),
                                    Commands.format(NamedTextColor.GREEN, String.format("Successfully unloaded world %s", properties.getKey())));
                        }
                    });
                    return CommandResult.success();
                })
                .build();
    }

    static Command.Parameterized rootCommand(final Random random) {
        return Command.builder()
                .setPermission(Constants.Plugin.ID + ".command.help")
                .setShortDescription(Component.text("Displays available commands"))
                .setExtendedDescription(Component.text("Displays available commands")) // TODO Do this better
                .setExecutor(context -> {
                    context.sendMessage(Identity.nil(), Component.text("Some help should go here..."));
                    return CommandResult.success();
                })
                .child(Commands.createCommand(random), "create", "c")
                // .child(Commands.registerCommand(), "register", "reg")
                .child(Commands.startCommand(), "start")
                .child(Commands.endCommand(), "end", "e")
                .child(Commands.joinCommand(), "join", "j")
                .child(Commands.reloadCommand(), "reload", "rel")
                .child(Commands.setCommand(), "set")
                .child(Commands.tpWorldCommand(), "tpworld", "tpw")
                .child(Commands.worldModifiedCommand(), "worldmodified", "modified", "wm")
                .child(Commands.loadWorldCommand(), "loadworld", "load", "lw")
                .child(Commands.unloadWorldCommand(), "unloadworld", "unload", "uw")
                .build();
    }

    private static TextComponent format(final NamedTextColor color, final String content) {
        return Component.text(content, color);
    }
}
