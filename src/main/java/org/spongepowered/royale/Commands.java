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

import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommonParameters;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.royale.configuration.MappedConfigurationAdapter;
import org.spongepowered.royale.instance.Instance;
import org.spongepowered.royale.instance.InstanceType;
import org.spongepowered.royale.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;

import java.io.IOException;
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
    private static final Parameter.Value<Boolean> FORCE_PARAMETER = Parameter.bool().key("force").optional().build();
    private static final Parameter.Value<Boolean> MODIFIED_PARAMETER = Parameter.bool().key("modified").optional().build();
    private static final Parameter.Value<SerializationBehavior> SERIALIZATION_BEHAVIOR_PARAMETER =
            Parameter.enumValue(SerializationBehavior.class).key("behavior").build();
    private static final Parameter.Value<List<ServerPlayer>> MANY_PLAYERS =
            Parameter.builder(new TypeToken<List<ServerPlayer>>() {}).addParser(CatalogedValueParameters.MANY_PLAYERS).optional()
                    .setKey("players").build();

    private static ServerWorld getWorld(final CommandContext context) throws CommandException {
        final Optional<ServerWorld> optWorldProperties = context.one(CommonParameters.WORLD);
        if (optWorldProperties.isPresent()) {
            if (!optWorldProperties.get().world().isLoaded()) {
                throw new CommandException(
                        Component.text("World [").append(format(NamedTextColor.GREEN, optWorldProperties.get().key().toString()))
                                .append(Component.text("] is not online.")));
            }
        } else if (context.cause().location().isPresent()) {
            return context.cause().location().get().world();
        } else {
            throw new CommandException(Component.text("World was not provided!"));
        }
    }

    private static Command.Parameterized createCommand(final Random random) {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.create")
                .shortDescription(Component.text("Creates an instance."))
                .extendedDescription(Component.text("Creates an instance from a ")
                        .append(format(NamedTextColor.GREEN, "world"))
                        .append(Component.text(" with the specified instance "))
                        .append(format(NamedTextColor.LIGHT_PURPLE, "type"))
                        .append(Component.text(".")))
                .addParameter(Commands.INSTANCE_TYPE_PARAMETER_OPTIONAL)
                .addParameter(CommonParameters.ALL_WORLD_PROPERTIES)
                .executor(context -> {
                    final InstanceType instanceType = context.one(Commands.INSTANCE_TYPE_PARAMETER_OPTIONAL).orElseGet(() -> {
                        return Sponge.server().registries().registry(Constants.Plugin.INSTANCE_TYPE).stream().findAny().get();
                    });
                    WorldProperties targetProperties = context.one(CommonParameters.ALL_WORLD_PROPERTIES).orElseGet(() -> {
                        final Optional<WorldProperties> properties = Sponge.server().worldManager().getProperties(instanceType.getKey());
                        return properties.orElse(null);
                    });

                    if (targetProperties != null && !targetProperties.getWorld().isPresent()) {
                        final ServerWorld loadedWorld = Sponge.server().worldManager().loadWorld(targetProperties).getNow(null);

                        if (loadedWorld == null) {
                            throw new CommandException(Commands.format(NamedTextColor.RED, String.format("Unable to find a world using instance type"
                                    + " id of %s. World could not be loaded!", instanceType.key())));
                        }

                        targetProperties = loadedWorld.properties();
                    }

                    if (targetProperties == null) {
                        throw new CommandException(
                                Commands.format(NamedTextColor.RED, String.format("Unable to find a world using instance type id of %s", instanceType
                                        .key())));
                    }

                    context.sendMessage(Identity.nil(),
                            Component.text().content("Creating an instance from [")
                                    .append(Commands.format(NamedTextColor.GREEN, targetProperties.getKey().asString()))
                                    .append(Component.text("] using instance type "))
                                    .append(Commands.format(NamedTextColor.LIGHT_PURPLE, instanceType.name()))
                                    .append(Component.text("."))
                                    .build()
                    );

                    final Instance currentInstance = Royale.instance.getInstanceManager().getInstance(targetProperties.getKey()).orElse(null);
                    if (currentInstance != null) {
                        throw new CommandException(Component.text().append(Component.text("World "), Component.text(currentInstance
                                        .getWorldKey().formatted(), NamedTextColor.DARK_PURPLE), Component.text(" is already an instance!"))
                                .build()
                        );
                    }

                    try {
                        Royale.instance.getInstanceManager().createInstance(targetProperties.getKey(), instanceType);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }

                    context.sendMessage(Identity.nil(),
                            Component.text().content("Created instance for [")
                                    .append(Commands.format(NamedTextColor.GREEN, targetProperties.getKey().asString()))
                                    .append(Component.text("]"))
                                    .build()
                    );

                    final WorldProperties actualProperties = targetProperties;
                    for (final ServerPlayer player : Sponge.server().onlinePlayers()) {
                        if (player.world().key().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                            player.sendMessage(Identity.nil(), Component.text().clickEvent(SpongeComponents.executeCallback(commandCause -> {
                                final Optional<Instance> inst = Royale.instance.getInstanceManager().getInstance(actualProperties.getKey());
                                if (inst.isPresent()) {
                                    final ServerPlayer serverPlayer = (ServerPlayer) commandCause.root();
                                    if (inst.get().registerPlayer(serverPlayer)) {
                                        inst.get().spawnPlayer(serverPlayer);
                                    }
                                }
                            })).append(LinearComponents
                                    .linear(Component.text("["), format(NamedTextColor.RED, targetProperties.getKey().toString()),
                                            Component.text("] is ready! Click this message or right-click a teleportation sign to join!"))).build());
                        }
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized startCommand() {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.start")
                .shortDescription(Component.text("Starts an ")
                        .append(format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text(".")))
                .extendedDescription(Component.text("Starts an ")
                        .append(format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text(".")))
                .addParameter(CommonParameters.WORLD)
                .executor(context -> {
                    final ServerWorld world =  context.one(CommonParameters.WORLD).orElseThrow(() -> new CommandException(Component.text("World was not provided!")));
                    final Optional<Instance> optInstance = Royale.instance.getInstanceManager().getInstance(world.key());
                    if (!optInstance.isPresent() || optInstance.get().getState().equals(Instance.State.IDLE)) {
                        try {
                            context.sendMessage(Identity.nil(),
                                    Component.text().content("Starting round countdown in [")
                                        .append(Commands.format(NamedTextColor.GREEN, world.key().toString()))
                                        .append(Component.text("]."))
                                        .build());
                            Royale.instance.getInstanceManager().startInstance(world.key());
                        } catch (final UnknownInstanceException e) {
                            throw new CommandException(Component.text().content("Unable to start round in [")
                                        .append(Commands.format(NamedTextColor.GREEN, world.key().toString()))
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
                .permission(Constants.Plugin.ID + ".command.end")
                .shortDescription(Component.text().content("Ends an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text("."))
                        .build())
                .addParameter(CommonParameters.WORLD)
                .addParameter(Commands.FORCE_PARAMETER)
                .executor(context -> {
                    final Optional<ServerWorld> optWorldProperties = context.one(CommonParameters.WORLD);
                    final ServerWorld world;
                    if (optWorldProperties.isPresent()) {
                        final ServerWorld opt = optWorldProperties.get().world();
                        if (!opt.isLoaded() && Royale.instance.getInstanceManager().getInstance(optWorldProperties.get().key()).isPresent()) {
                            context.sendMessage(Identity.nil(),
                                    Component.text(String.format("World %s was unloaded, but the instance still exists! Ending instance.",
                                            optWorldProperties.get().key()),
                                            NamedTextColor.YELLOW));
                            try {
                                Royale.instance.getInstanceManager().endInstance(optWorldProperties.get().key(), true);
                            } catch (final UnknownInstanceException e) {
                                e.printStackTrace();
                            }
                            return CommandResult.empty();
                        }
                        world = opt.orElseThrow(() -> new CommandException(Component.text().content("World [")
                                .append(Commands.format(NamedTextColor.GREEN, optWorldProperties.get().key().toString()))
                                .append(Component.text("] is not online."))
                                .build()));
                    } else {
                        world = context.cause().location().map(Location::world).orElseThrow(() ->
                                new CommandException(Component.text("World was not provided!")));
                    }

                    final boolean force = context.one(Commands.FORCE_PARAMETER).orElse(false);

                    try {
                        context.sendMessage(Identity.nil(), Component.text()
                                .content(force ? "Forcibly e" : "E")
                                .append(Component.text("nding round in ["))
                                .append(Commands.format(NamedTextColor.GREEN, world.key().asString()))
                                .append(Component.text("]."))
                                .build());
                        Royale.instance.getInstanceManager().endInstance(world.key(), force);
                    } catch (final UnknownInstanceException e) {
                        throw new CommandException(
                                Component.text().content("Unable to end round in [")
                                    .append(Commands.format(NamedTextColor.GREEN, world.key().asString()))
                                    .append(Component.text("]!"))
                                    .build());
                    }

                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized joinCommand() {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.join")
                .shortDescription(Component.text()
                        .content("Joins an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text("."))
                        .build())
                .addParameter(Parameter.firstOfBuilder(Parameter.seq(CommonParameters.WORLD, CommonParameters.PLAYER_OPTIONAL))
                        .or(CommonParameters.PLAYER_OPTIONAL).optional().build())
                .executor(context -> {
                    final ServerWorld world = Commands.getWorld(context);

                    final Optional<ServerPlayer> playerReturn = context.one(CommonParameters.PLAYER_OPTIONAL);
                    final ServerPlayer player;
                    if (playerReturn.isPresent()) {
                        player = playerReturn.get();
                    } else {
                        if (context.cause().root() instanceof ServerPlayer) {
                            player = (ServerPlayer) context.cause().root();
                        } else {
                            context.sendMessage(Identity.nil(),
                                    Component.text("A player needs to be provided if you are not a player!", NamedTextColor.RED));
                            return CommandResult.empty();
                        }
                    }

                    final Optional<Instance> instance = Royale.instance.getInstanceManager().getInstance(world.key());
                    if (!instance.isPresent()) {
                        throw new CommandException(
                                Component.text().content("Instance [")
                                    .append(Commands.format(NamedTextColor.GREEN, world.key().asString()))
                                    .append(Component.text("] is not a valid instance, is it running?"))
                                    .build());
                    }

                    player.sendMessage(Identity.nil(), Component.text().content("Joining [")
                            .append(Commands.format(NamedTextColor.GREEN, world.key().asString()))
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
                .permission(Constants.Plugin.ID + ".command.reload")
                .shortDescription(Component.text()
                        .content("Reloads the configuration of an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance type"))
                        .append(Component.text("."))
                        .build())
                .addParameter(Commands.INSTANCE_TYPE_PARAMETER)
                .executor(context -> {
                    final InstanceType instanceType = context.requireOne(Commands.INSTANCE_TYPE_PARAMETER);
                    final Path configPath = Constants.Map.INSTANCE_TYPES_FOLDER.resolve(instanceType.getKey().value() + ".conf");
                    final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(
                            InstanceTypeConfiguration.class, Royale.instance.getConfigurationOptions(), configPath);

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
                .permission(Constants.Plugin.ID + ".command.set.serialization")
                .shortDescription(Component.text().content("Sets the serialization property of an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text("."))
                        .build())
                .addParameter(CommonParameters.WORLD)
                .addParameter(Commands.SERIALIZATION_BEHAVIOR_PARAMETER)
                .executor(context -> {
                    final ServerWorld world = Commands.getWorld(context);
                    final SerializationBehavior serializationBehavior = context.requireOne(Commands.SERIALIZATION_BEHAVIOR_PARAMETER);

                    world.properties().setSerializationBehavior(serializationBehavior);
                    context.sendMessage(Identity.nil(), Component.text().content("World [")
                            .append(Commands.format(NamedTextColor.GREEN, world.key().asString()))
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
                .permission(Constants.Plugin.ID + ".command.set")
                .shortDescription(Component.text().content("Sets a property of an ")
                        .append(Commands.format(NamedTextColor.LIGHT_PURPLE, "instance"))
                        .append(Component.text("."))
                        .build())
                .addChild(Commands.setSerializationCommand(), "serialization")
                .build();
    }

    private static Command.Parameterized tpWorldCommand() {
        return Command.builder()
                .shortDescription(Component.text().content("Teleports a player to another ")
                        .append(Commands.format(NamedTextColor.GREEN, "world"))
                        .append(Component.text("."))
                        .build())
                .addParameter(Commands.MANY_PLAYERS)
                .addParameter(CommonParameters.WORLD)
                .permission(Constants.Plugin.ID + ".command.tpworld")
                .executor(context -> {
                    final ServerWorld world = Commands.getWorld(context);
                    final Optional<List<ServerPlayer>> playerReturn = context.one(Commands.MANY_PLAYERS);
                    final Collection<ServerPlayer> players;
                    if (playerReturn.isPresent()) {
                        players = playerReturn.get();
                    } else {
                        if (context.cause().root() instanceof ServerPlayer) {
                            players = Collections.singletonList((ServerPlayer) context.cause().root());
                        } else {
                            context.sendMessage(Identity.nil(),
                                    Component.text("Players need to be provided if you are not a player!", NamedTextColor.RED));
                            return CommandResult.empty();
                        }
                    }
                    for (final ServerPlayer target : players) {
                        target.setLocation(world.engine().teleportHelper().findSafeLocation(ServerLocation.of(world,
                                world.properties().spawnPosition())).orElseGet(() -> ServerLocation.of(world, world.properties()
                                .spawnPosition())));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized loadWorldCommand() {
        return Command.builder()
                .shortDescription(Component.text("Manually loads a world"))
                .addParameter(CommonParameters.ALL_WORLD_PROPERTIES)
                .permission(Constants.Plugin.ID + ".command.worldload")
                .executor(context -> {
                    final WorldProperties properties = context.requireOne(CommonParameters.ALL_WORLD_PROPERTIES);
                    if (properties.getWorld().isPresent()) {
                        throw new CommandException(Commands.format(NamedTextColor.YELLOW, String.format("World %s is already loaded!", properties.getKey())));
                    }

                    context.sendMessage(Identity.nil(),
                            Commands.format(NamedTextColor.GREEN, String.format("Loading world %s...", properties.getKey())));
                    final CompletableFuture<ServerWorld> future = Sponge.server().worldManager().loadWorld(properties);
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
                .shortDescription(Component.text("Manually unloads a world"))
                .addParameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY)
                .permission(Constants.Plugin.ID + ".command.unloadworld")
                .executor(context -> {
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
                    final CompletableFuture<Boolean> unloadFuture = Sponge.server().worldManager().unloadWorld(properties.getKey());
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
                .permission(Constants.Plugin.ID + ".command.root")
                .shortDescription(Component.text("Displays available commands"))
                .extendedDescription(Component.text("Displays available commands"))
                .executor(context -> {
                    context.sendMessage(Identity.nil(), Component.text("Some help should go here..."));
                    return CommandResult.success();
                })
                .addChild(Commands.createCommand(random), "create", "c")
                .addChild(Commands.startCommand(), "start")
                .addChild(Commands.endCommand(), "end", "e")
                .addChild(Commands.joinCommand(), "join", "j")
                .addChild(Commands.reloadCommand(), "reload", "rel")
                .addChild(Commands.setCommand(), "set")
                .addChild(Commands.tpWorldCommand(), "tpworld", "tpw")
                .addChild(Commands.loadWorldCommand(), "loadworld", "load", "lw")
                .addChild(Commands.unloadWorldCommand(), "unloadworld", "unload", "uw")
                .build();
    }

    private static TextComponent format(final NamedTextColor color, final String content) {
        return Component.text(content, color);
    }
}
