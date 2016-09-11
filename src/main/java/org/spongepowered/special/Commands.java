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

import static org.spongepowered.api.command.args.GenericArguments.bool;
import static org.spongepowered.api.command.args.GenericArguments.catalogedElement;
import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.player;
import static org.spongepowered.api.command.args.GenericArguments.playerOrSource;
import static org.spongepowered.api.command.args.GenericArguments.seq;
import static org.spongepowered.api.command.args.GenericArguments.string;
import static org.spongepowered.api.command.args.GenericArguments.world;

import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.SerializationBehaviors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.special.configuration.MappedConfigurationAdapter;
import org.spongepowered.special.instance.Instance;
import org.spongepowered.special.instance.InstanceType;
import org.spongepowered.special.instance.InstanceTypeRegistryModule;
import org.spongepowered.special.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.special.instance.exception.UnknownInstanceException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

final class Commands {

    private static final CommandSpec createCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.create")
            .description(Text.of("Creates an instance."))
            .extendedDescription(Text.of("Creates an instance from a ", format(TextColors.GREEN, "world"), " with the specified instance ",
                    format(TextColors.LIGHT_PURPLE, "type"), "."))
            .arguments(catalogedElement(Text.of("instanceType"), InstanceType.class), world(Text.of("targetWorld")))
            .executor((src, args) -> {
                final InstanceType instanceType = args.<InstanceType>getOne("instanceType").orElse(null);
                final WorldProperties targetProperties = args.<WorldProperties>getOne("targetWorld").orElse(null);

                src.sendMessage(Text.of("Creating an instance from [", format(TextColors.GREEN, targetProperties.getWorldName()), "] using instance ",
                                "type ", format(TextColors.LIGHT_PURPLE, instanceType.getName()), "."));

                try {
                    Special.instance.getInstanceManager().createInstance(targetProperties.getWorldName(), instanceType);
                } catch (Exception e) {
                    throw new CommandException(Text.of(e));
                }

                src.sendMessage(Text.of("Created instance for [", format(TextColors.GREEN, targetProperties.getWorldName()), "]."));
                return CommandResult.success();
            })
            .build();

    private static final CommandSpec registerCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.register")
            .description(Text.of("Registers an ", format(TextColors.LIGHT_PURPLE, "instance type"), "."))
            .extendedDescription(Text.of("Registers an ", format(TextColors.LIGHT_PURPLE, "instance type"), " using a specified ID and/or name."))
            .arguments(string(Text.of("id")), optional(string(Text.of("name"))))
            .executor((src, args) -> {
                final String id = args.<String>getOne("id").orElse(null);
                if (Sponge.getRegistry().getType(InstanceType.class, id).isPresent()) {
                    throw new CommandException(Text.of("Unable to register [", format(TextColors.LIGHT_PURPLE, id), "] as an instance with this ",
                            "name is already registered."));
                }

                final String name = args.<String>getOne("name").orElse(null);

                try {
                    InstanceTypeRegistryModule.getInstance().registerAdditionalCatalog(InstanceType.builder().build(id, name));
                    src.sendMessage(Text.of("Registered instance type [", format(TextColors.LIGHT_PURPLE, id), "]."));
                } catch (IOException | ObjectMappingException e) {
                    throw new CommandException(Text.of("Failed to register instance [", format(TextColors.LIGHT_PURPLE, id), "].", e));
                }
                return CommandResult.success();
            })
            .build();

    private static final CommandSpec startCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.start")
            .description(Text.of("Starts an ", format(TextColors.LIGHT_PURPLE, "instance"), "."))
            .extendedDescription(Text.of("Starts an ", format(TextColors.LIGHT_PURPLE, "instance"), "."))
            .arguments(optional(world(Text.of("targetWorld"))))
            .executor((src, args) -> {
                final Optional<WorldProperties> optWorldProperties = args.getOne("targetWorld");
                final World world;
                if (optWorldProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optWorldProperties.get().getUniqueId()).orElseThrow(() -> new CommandException(Text.of(
                            "World [", format(TextColors.GREEN, optWorldProperties.get().getWorldName()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }

                final Optional<Instance> optInstance = Special.instance.getInstanceManager().getInstance(world.getName());
                if (!optInstance.isPresent() || optInstance.isPresent() && optInstance.get().getState().equals(Instance.State.IDLE)) {
                    try {
                        src.sendMessage(Text.of("Starting round countdown in [", format(TextColors.GREEN, world.getName()), "]."));
                        Special.instance.getInstanceManager().startInstance(world.getName());
                    } catch (UnknownInstanceException e) {
                        throw new CommandException(Text.of("Unable to start round in [", format(TextColors.GREEN, world.getName()), "], was it ",
                                "created?"));
                    }
                } else {
                    src.sendMessage(Text.of("Round already in progress."));
                }

                return CommandResult.success();
            })
            .build();

    private static final CommandSpec endCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.end")
            .description(Text.of("Ends an ", format(TextColors.LIGHT_PURPLE, "instance"), "."))
            .extendedDescription(Text.of("Ends an ", format(TextColors.LIGHT_PURPLE, "instance"), "."))
            .arguments(optional(world(Text.of("targetWorld"))), optional(bool(Text.of("force"))))
            .executor((src, args) -> {
                final Optional<WorldProperties> optWorldProperties = args.getOne("targetWorld");
                final World world;
                if (optWorldProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optWorldProperties.get().getUniqueId()).orElseThrow(() -> new CommandException(Text.of(
                            "World [", format(TextColors.GREEN, optWorldProperties.get().getWorldName()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }

                boolean force = args.<Boolean>getOne("force").orElse(false);

                try {
                    src.sendMessage(Text.of((force ? "Forcibly e" : "E"), "nding round in [", format(TextColors.GREEN, world.getName()), "]."));
                    Special.instance.getInstanceManager().endInstance(world.getName(), force);
                } catch (UnknownInstanceException e) {
                    throw new CommandException(Text.of("Unable to end round in [", format(TextColors.GREEN, world.getName()), "]!"));
                }

                return CommandResult.success();
            })
            .build();

    private static final CommandSpec joinCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.join")
            .description(Text.of("Joins an ", format(TextColors.LIGHT_PURPLE, "instance"), "."))
            .extendedDescription(Text.of("Joins an ", format(TextColors.LIGHT_PURPLE, "instance"), "."))
            .arguments(optional(world(Text.of("targetWorld"))), optional(player(Text.of("player"))))
            .executor((src, args) -> {
                final Optional<WorldProperties> optWorldProperties = args.getOne("targetWorld");
                final World world;
                if (optWorldProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optWorldProperties.get().getUniqueId()).orElseThrow(() -> new CommandException(Text.of(
                            "World [", format(TextColors.GREEN, optWorldProperties.get().getWorldName()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }

                final Optional<Player> optPlayer = args.getOne("player");
                final Player player;
                if (optPlayer.isPresent()) {
                    player = optPlayer.get();
                } else if (src instanceof Player) {
                    player = (Player) src;
                } else {
                    throw new CommandException(Text.of("Player was not specified and source was not a player!"));
                }

                final Optional<Instance> instance = Special.instance.getInstanceManager().getInstance(world.getName());
                if (!instance.isPresent()) {
                    throw new CommandException(Text.of("Instance [", format(TextColors.GREEN, world.getName()), "] is not a valid instance, is it ",
                            "running?"));
                }

                player.sendMessage(Text.of("Joining [", format(TextColors.GREEN, world.getName()), "]."));
                instance.get().spawnPlayer(player);

                return CommandResult.success();
            })
            .build();

    private static final CommandSpec reloadCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.reload")
            .description(Text.of("Reloads the configuration of an ", format(TextColors.LIGHT_PURPLE, "instance type"), "."))
            .extendedDescription(Text.of("Reloads the configuration an ", format(TextColors.LIGHT_PURPLE, "instance type"), "."))
            .arguments(catalogedElement(Text.of("instanceType"), InstanceType.class))
            .executor((src, args) -> {
                final InstanceType instanceType = args.<InstanceType>getOne("instanceType").orElseThrow(() -> new CommandException(Text.of
                        ("Specified instance is not valid!")));

                final Path configPath = Constants.Map.PATH_CONFIG_INSTANCE_TYPES.resolve(instanceType.getId() + ".conf");
                final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(
                        InstanceTypeConfiguration.class, Constants.Map.DEFAULT_OPTIONS, configPath);

                try {
                    adapter.load();
                } catch (IOException | ObjectMappingException e) {
                    throw new CommandException(Text.of("Unable to load configuration for instance type [", format(TextColors.LIGHT_PURPLE, instanceType
                            .getId()), "]."));
                }

                instanceType.injectFromConfig(adapter.getConfig());

                src.sendMessage(Text.of("Reloaded configuration for instance type [", format(TextColors.LIGHT_PURPLE, instanceType.getId()), "]."));

                return CommandResult.success();
            })
            .build();

    private static final CommandSpec setSerializationCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.set.serialization")
            .description(Text.of("Sets the serialization property of an ", format(TextColors.LIGHT_PURPLE, "instance"), "."))
            .arguments(optional(world(Text.of("targetWorld"))), catalogedElement(Text.of("serializationBehavior"), SerializationBehavior.class))
            .executor((src, args) -> {
                final Optional<WorldProperties> optWorldProperties = args.getOne("targetWorld");
                final World world;
                if (optWorldProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optWorldProperties.get().getUniqueId()).orElseThrow(() -> new CommandException(Text.of(
                            "World [", format(TextColors.GREEN, optWorldProperties.get().getWorldName()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }

                final SerializationBehavior serializationBehavior = args.<SerializationBehavior>getOne("serializationBehavior").orElseThrow(() ->
                        new CommandException(Text.of("Invalid serialization behavior!")));

                world.setSerializationBehavior(serializationBehavior);
                src.sendMessage(Text.of("World [", format(TextColors.GREEN, world.getName()), "] set to serialization behavior [", format
                        (TextColors.YELLOW, serializationBehavior.getName()), "]."));

                return CommandResult.success();
            })
            .build();

    private static final CommandSpec setCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.set")
            .description(Text.of("Sets a property of an ", format(TextColors.LIGHT_PURPLE, "instance"), "."))
            .child(setSerializationCommand, "serialization")
            .build();

    private static final CommandSpec tpWorldCommand = CommandSpec.builder()
            .description(Text.of("Teleports a player to another ", format(TextColors.GREEN, "world"), "."))
            .arguments(seq(playerOrSource(Text.of("targetPlayer")), onlyOne(world(Text.of("targetWorld")))))
            .permission(Constants.Meta.ID + ".command.tpworld")
            .executor((src, args) -> {
                final Optional<WorldProperties> optWorldProperties = args.getOne("targetWorld");
                final World world;
                if (optWorldProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optWorldProperties.get().getUniqueId()).orElseThrow(() -> new CommandException(Text.of(
                            "World [", format(TextColors.GREEN, optWorldProperties.get().getWorldName()), "] is not online.")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }
                for (Player target : args.<Player>getAll("targetPlayer")) {
                    target.setLocation(new Location<>(world, world.getProperties().getSpawnPosition()));
                }
                return CommandResult.success();
            })
            .build();

    static final CommandSpec rootCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.help")
            .description(Text.of("Displays available commands"))
            .extendedDescription(Text.of("Displays available commands")) // TODO Do this better
            .executor((src, args) -> {
                src.sendMessage(Text.of("Some help should go here..."));
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
            .build();

    private static Text format(TextColor color, String content) {
        return Text.of(color, content, TextColors.RESET);
    }
}
