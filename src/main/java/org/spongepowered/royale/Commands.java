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
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionType;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionTypes;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.royale.configuration.MappedConfigurationAdapter;
import org.spongepowered.royale.instance.InstanceImpl;
import org.spongepowered.royale.instance.InstanceType;
import org.spongepowered.royale.api.RoyaleKeys;
import org.spongepowered.royale.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

final class Commands {

    private static final Parameter.Value<InstanceType> INSTANCE_TYPE_PARAMETER =
            Parameter.registryElement(TypeToken.get(InstanceType.class),
                    Constants.Plugin.INSTANCE_TYPE.asDefaultedType(() -> Sponge.server().registries())).key("instanceType").build();
    private static final Parameter.Value<Boolean> FORCE_PARAMETER = Parameter.bool().key("force").optional().build();
    private static final Parameter.Value<ResourceKey> WORLD_KEY_PARAMETER = Parameter.builder(ResourceKey.class)
            .key("world key")
            .addParser(new WorldKeyParameter())
            .build();
    private static final Parameter.Value<ResourceKey> INSTANCE_KEY_PARAMETER = Parameter.builder(ResourceKey.class)
            .key("instance key")
            .addParser(new InstanceKeyParameter())
            .build();

    private static Command.Parameterized createCommand() {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.create")
                .shortDescription(Component.text("Creates an instance."))
                .extendedDescription(Component.text("Creates an instance from a ")
                        .append(Component.text("world", NamedTextColor.GREEN))
                        .append(Component.text(" with the specified instance "))
                        .append(Component.text("type", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(".")))
                .addParameter(Commands.WORLD_KEY_PARAMETER)
                .addParameter(Commands.INSTANCE_TYPE_PARAMETER)
                .addParameter(Commands.FORCE_PARAMETER)
                .executor(context -> {
                    final InstanceType instanceType = context.requireOne(Commands.INSTANCE_TYPE_PARAMETER);
                    final ResourceKey targetWorldKey = context.requireOne(Commands.WORLD_KEY_PARAMETER);
                    final boolean force = context.one(Commands.FORCE_PARAMETER).orElse(false);

                    context.sendMessage(Identity.nil(), Component.text().content("Creating an instance from [")
                            .append(Component.text(targetWorldKey.formatted(), NamedTextColor.GREEN))
                            .append(Component.text("] using instance type "))
                            .append(Component.text(instanceType.name(), NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text("."))
                            .build());

                    Royale.getInstance().getInstanceManager().createInstance(targetWorldKey, instanceType, force)
                            .whenCompleteAsync((instance, throwable) -> {
                                if (throwable != null) {
                                    context.sendMessage(Identity.nil(), Component.text(throwable.getMessage(), NamedTextColor.RED));
                                    Royale.getInstance().getPlugin().getLogger().error(throwable);
                                    return;
                                }
                                context.sendMessage(Identity.nil(),
                                        Component.text().content("Created instance for [")
                                                .append(Component.text(targetWorldKey.formatted(), NamedTextColor.GREEN))
                                                .append(Component.text("]"))
                                                .build()
                                );
                                for (final ServerPlayer player : Sponge.server().onlinePlayers()) {
                                    if (player.world().key().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                                        player.sendMessage(Identity.nil(), Component.text().clickEvent(SpongeComponents.executeCallback(commandCause -> {
                                            final Optional<InstanceImpl> inst = Royale.getInstance().getInstanceManager().getInstance(targetWorldKey);
                                            if (inst.isPresent()) {
                                                final ServerPlayer serverPlayer = (ServerPlayer) commandCause.root();
                                                if (inst.get().registerPlayer(serverPlayer)) {
                                                    inst.get().spawnPlayer(serverPlayer);
                                                }
                                            }
                                        })).append(LinearComponents
                                                .linear(Component.text("["), Component.text(targetWorldKey.formatted(), NamedTextColor.RED),
                                                        Component.text("] is ready! Click this message or right-click a teleportation sign to join!"))).build());
                                    }
                                }
                            }, Royale.getInstance().getTaskExecutorService());
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized statusCommand() {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.status")
                .executor(context -> {
                    Component msg = Component.text("Lobby: ").append(worldStatus(Constants.Map.Lobby.LOBBY_WORLD_KEY));
                    for (InstanceImpl instance : Royale.getInstance().getInstanceManager().getAll()) {
                        msg = msg.append(Component.newline())
                                .append(Component.text("Instance " + instance.getWorldKey() + ": ")
                                .append(worldStatus(instance.getWorldKey())))
                                .append(Component.text(" (" + instance.getState() + ") "));
                    }
                    context.sendMessage(Identity.nil(), msg);
                    return CommandResult.success();
                })
                .build();
    }

    private static Command.Parameterized linkCommand() {
        return Command.builder()
                .addParameter(Commands.WORLD_KEY_PARAMETER)
                .addParameter(Commands.INSTANCE_TYPE_PARAMETER)
                .permission(Constants.Plugin.ID + ".command.link")
                .executor(context -> {
                    if (!(context.cause().root() instanceof ServerPlayer)) {
                        throw new CommandException(Component.text("A player needs to be provided if you are not a player!", NamedTextColor.RED));
                    }

                    final ServerPlayer player = (ServerPlayer) context.cause().root();
                    final ResourceKey targetWorldKey = context.requireOne(Commands.WORLD_KEY_PARAMETER);
                    final InstanceType type = context.requireOne(Commands.INSTANCE_TYPE_PARAMETER);
                    final ItemStack stack = player.itemInHand(HandTypes.MAIN_HAND);
                    if (stack.type().isAnyOf(ItemTypes.ACACIA_SIGN, ItemTypes.BIRCH_SIGN, ItemTypes.DARK_OAK_SIGN, ItemTypes.JUNGLE_SIGN, ItemTypes.OAK_SIGN, ItemTypes.SPRUCE_SIGN, ItemTypes.CRIMSON_SIGN, ItemTypes.WARPED_SIGN)) {
                        stack.offer(RoyaleKeys.WORLD, targetWorldKey);
                        stack.offer(RoyaleKeys.TYPE, type.key());
                        player.setItemInHand(HandTypes.MAIN_HAND, stack);
                        context.sendMessage(Identity.nil(), Component.text("Place down your sign to link it to the instance", NamedTextColor.YELLOW));
                    } else {
                        context.sendMessage(Identity.nil(), Component.text("You need to hold a sign in your hand", NamedTextColor.RED));
                    }
                    return CommandResult.success();
                })
                .build();
    }

    private static Component worldStatus(ResourceKey key) {
        boolean exists = Sponge.server().worldManager().worldExists(key);
        boolean loaded = Sponge.server().worldManager().world(key).isPresent();
        return exists ? (loaded ? Component.text("Loaded") : Component.text("Unloaded")) : Component.text("Missing");
    }

    private static Command.Parameterized startCommand() {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.start")
                .shortDescription(Component.text("Starts an ")
                        .append(Component.text("instance", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(".")))
                .extendedDescription(Component.text("Starts an ")
                        .append(Component.text("instance", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(".")))
                .addParameter(Commands.INSTANCE_KEY_PARAMETER)
                .executor(context -> {
                    final ResourceKey worldKey =  context.requireOne(Commands.INSTANCE_KEY_PARAMETER);
                    try {
                        context.sendMessage(Identity.nil(),
                                Component.text().content("Starting round countdown in [")
                                        .append(Component.text(worldKey.formatted(), NamedTextColor.GREEN))
                                        .append(Component.text("]."))
                                        .build());
                        Royale.getInstance().getInstanceManager().startInstance(worldKey);
                        return CommandResult.success();
                    } catch (final UnknownInstanceException e) {
                        throw new CommandException(Component.text().content("Unable to start round in [")
                                .append(Component.text(worldKey.formatted(), NamedTextColor.GREEN))
                                .append(Component.text("], was it created?"))
                                .build());
                    }
                })
                .build();
    }

    private static Command.Parameterized endCommand() {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.end")
                .shortDescription(Component.text().content("Ends an ")
                        .append(Component.text("instance", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text("."))
                        .build())
                .addParameter(Commands.INSTANCE_KEY_PARAMETER)
                .addParameter(Commands.FORCE_PARAMETER)
                .executor(context -> {
                    final ResourceKey worldKey = context.requireOne(Commands.INSTANCE_KEY_PARAMETER);
                    final boolean force = context.one(Commands.FORCE_PARAMETER).orElse(false);

                    context.sendMessage(Identity.nil(), Component.text()
                            .content(force ? "Forcibly e" : "E")
                            .append(Component.text("nding round in ["))
                            .append(Component.text(worldKey.formatted(), NamedTextColor.GREEN))
                            .append(Component.text("]."))
                            .build());

                    context.sendMessage(Identity.nil(),
                            Component.text(String.format("World %s was unloaded, but the instance still exists! Ending instance.",
                                    worldKey),
                                    NamedTextColor.YELLOW));
                    try {
                        Royale.getInstance().getInstanceManager().endInstance(worldKey, force);
                        return CommandResult.success();
                    } catch (final UnknownInstanceException e) {
                        throw new CommandException(Component.text().content("Unable to end round in [")
                                .append(Component.text(worldKey.formatted(), NamedTextColor.GREEN))
                                .append(Component.text("], was it created?"))
                                .build());
                    }
                })
                .build();
    }

    private static Command.Parameterized joinCommand() {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.join")
                .shortDescription(Component.text()
                        .content("Joins an ")
                        .append(Component.text("instance", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text("."))
                        .build())
                .addParameter(Commands.INSTANCE_KEY_PARAMETER)
                .executor(context -> {
                    if (!(context.cause().root() instanceof ServerPlayer)) {
                        throw new CommandException(Component.text("A player needs to be provided if you are not a player!", NamedTextColor.RED));
                    }

                    final ServerPlayer player = (ServerPlayer) context.cause().root();
                    final ResourceKey worldKey = context.requireOne(Commands.INSTANCE_KEY_PARAMETER);

                    final Optional<InstanceImpl> instance = Royale.getInstance().getInstanceManager().getInstance(worldKey);
                    if (!instance.isPresent()) {
                        throw new CommandException(
                                Component.text().content("Instance [")
                                    .append(Component.text(worldKey.formatted(), NamedTextColor.GREEN))
                                    .append(Component.text("] is not a valid instance, is it running?"))
                                    .build());
                    }

                    player.sendMessage(Identity.nil(), Component.text().content("Joining [")
                            .append(Component.text(worldKey.formatted(), NamedTextColor.GREEN))
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
                        .append(Component.text("instance type", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text("."))
                        .build())
                .addParameter(Commands.INSTANCE_TYPE_PARAMETER)
                .executor(context -> {
                    final InstanceType instanceType = context.requireOne(Commands.INSTANCE_TYPE_PARAMETER);
                    final Path configPath = Constants.Map.INSTANCE_TYPES_FOLDER.resolve(instanceType.key().value() + ".conf");
                    final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(
                            InstanceTypeConfiguration.class, Royale.getInstance().getConfigurationOptions(), configPath);

                    try {
                        adapter.load();
                    } catch (final ConfigurateException e) {
                        throw new CommandException(
                                Component.text().content("Unable to load configuration for instance type [")
                                    .append(Component.text(instanceType.key().formatted(), NamedTextColor.LIGHT_PURPLE))
                                    .append(Component.text("]."))
                                    .build());
                    }

                    instanceType.injectFromConfig(adapter.getConfig());

                    context.sendMessage(Identity.nil(), Component.text().content("Reloaded configuration for instance type [")
                            .append(Component.text(instanceType.key().formatted(), NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text("]."))
                            .build());

                    return CommandResult.success();
                })
                .build();
    }

    static Command.Parameterized rootCommand() {
        return Command.builder()
                .permission(Constants.Plugin.ID + ".command.root")
                .shortDescription(Component.text("Displays available commands"))
                .extendedDescription(Component.text("Displays available commands"))
                .executor(context -> {
                    context.sendMessage(Identity.nil(), Component.text("Some help should go here..."));
                    return CommandResult.success();
                })
                .addChild(Commands.createCommand(), "create")
                .addChild(Commands.statusCommand(), "status")
                .addChild(Commands.linkCommand(), "link")
                .addChild(Commands.startCommand(), "start")
                .addChild(Commands.endCommand(), "end")
                .addChild(Commands.joinCommand(), "join")
                .addChild(Commands.reloadCommand(), "reload")
                .build();
    }

    private static class WorldKeyParameter implements ValueParameter<ResourceKey> {

        @Override
        public List<String> complete(final CommandContext context, final String currentInput) {
            return Sponge.server().worldManager().worldKeys().stream()
                    .map(ResourceKey::formatted)
                    .filter(x -> x.startsWith(currentInput.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<? extends ResourceKey> parseValue(final Parameter.Key<? super ResourceKey> parameterKey,
                final ArgumentReader.Mutable reader,
                final CommandContext.Builder context) throws ArgumentParseException {
            final ResourceKey key = reader.parseResourceKey();
            if (Sponge.server().worldManager().worldExists(key)) {
                return Optional.of(key);
            }
            throw reader.createException(Component.text(String.format("The world %s does not exist.", key.formatted()), NamedTextColor.RED));
        }

        @Override
        public List<ClientCompletionType> clientCompletionType() {
            return Collections.singletonList(ClientCompletionTypes.RESOURCE_KEY.get());
        }
    }

    private static class InstanceKeyParameter implements ValueParameter<ResourceKey> {

        @Override
        public List<String> complete(final CommandContext context, final String currentInput) {
            return Royale.getInstance().getInstanceManager().getAll().stream().map(InstanceImpl::getWorldKey)
                    .map(ResourceKey::formatted)
                    .filter(x -> x.startsWith(currentInput.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<? extends ResourceKey> parseValue(final Parameter.Key<? super ResourceKey> parameterKey,
                final ArgumentReader.Mutable reader,
                final CommandContext.Builder context) throws ArgumentParseException {
            final ResourceKey key = reader.parseResourceKey();
            if (Sponge.server().worldManager().worldExists(key)) {
                final Optional<InstanceImpl> instance = Royale.getInstance().getInstanceManager().getInstance(key);
                if (instance.isPresent()) {
                    return Optional.of(key);
                }
                throw reader.createException(Component.text(String.format("The world %s is not an instance.", key.formatted()), NamedTextColor.RED));
            }
            throw reader.createException(Component.text(String.format("The world %s does not exist.", key.formatted()), NamedTextColor.RED));
        }

        @Override
        public List<ClientCompletionType> clientCompletionType() {
            return Collections.singletonList(ClientCompletionTypes.RESOURCE_KEY.get());
        }
    }
}
