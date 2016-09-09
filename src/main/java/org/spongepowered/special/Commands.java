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
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.player;
import static org.spongepowered.api.command.args.GenericArguments.string;
import static org.spongepowered.api.command.args.GenericArguments.world;

import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.special.instance.Instance;
import org.spongepowered.special.instance.InstanceType;
import org.spongepowered.special.instance.InstanceTypeRegistryModule;
import org.spongepowered.special.instance.exception.InstanceAlreadyExistsException;
import org.spongepowered.special.instance.exception.UnknownInstanceException;

import java.io.IOException;
import java.util.Optional;

final class Commands {

    private static final CommandSpec registerCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.register")
            .description(Text.of())
            .extendedDescription(Text.of())
            .arguments(string(Text.of("id")), optional(string(Text.of("name"))))
            .executor((src, args) -> {
                final String id = args.<String>getOne("id").orElse(null);
                if (Sponge.getRegistry().getType(InstanceType.class, id).isPresent()) {
                    throw new CommandException(Text.of("Map has already been registered!"));
                }

                final String name = args.<String>getOne("name").orElse(id);

                try {
                    InstanceTypeRegistryModule.getInstance().registerAdditionalCatalog(InstanceType.builder().build(id, name));
                } catch (IOException | ObjectMappingException e) {
                    throw new CommandException(Text.of("Failed to create instance type [", id, "]!", e));
                }

                return CommandResult.success();
            })
            .build();

    private static final CommandSpec createCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.create")
            .description(Text.of("Creates an instance"))
            .extendedDescription(Text.of("Creates an instance")) // TODO More descriptive
            .arguments(catalogedElement(Text.of("instanceType"), InstanceType.class), world(Text.of("world")))
            .executor((src, args) -> {
                final InstanceType instanceType = args.<InstanceType>getOne("instanceType").orElse(null);
                final WorldProperties instanceProperties = args.<WorldProperties>getOne("world").orElse(null);

                try {
                    Special.instance.getInstanceManager().createInstance(instanceProperties.getWorldName(), instanceType);
                } catch (InstanceAlreadyExistsException e) {
                    throw new CommandException(Text.of(e));
                } catch (IOException e) {
                    throw new CommandException(Text.of("Unable to create instance!"), e);
                }

                return CommandResult.success();
            })
            .build();

    private static final CommandSpec startCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.start")
            .description(Text.of("Starts an instance"))
            .extendedDescription(Text.of("Starts an instance")) // TODO More descriptive
            .arguments(optional(world(Text.of("world"))))
            .executor((src, args) -> {
                final Optional<WorldProperties> optProperties = args.getOne("world");
                final World world;
                if (optProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optProperties.get().getWorldName()).orElseThrow(() -> new
                            CommandException(Text.of("World provided is not online!")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }

                try {
                    Special.instance.getInstanceManager().startInstance(world.getName());
                } catch (UnknownInstanceException e) {
                    throw new CommandException(Text.of("Unable to start instance [" + world.getName() + "], has it been prepared?"), e);
                }

                return CommandResult.success();
            })
            .build();

    private static final CommandSpec endCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.end")
            .description(Text.of("Ends an instance"))
            .extendedDescription(Text.of("Ends an instance")) // TODO More descriptive
            .arguments(optional(world(Text.of("world"))), optional(bool(Text.of("force"))))
            .executor((src, args) -> {
                final Optional<WorldProperties> optProperties = args.getOne("world");
                final World world;
                if (optProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optProperties.get().getWorldName()).orElseThrow(() -> new
                            CommandException(Text.of("World provided is not online!")));
                } else if (src instanceof Player) {
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }

                final Optional<Boolean> optForce = args.getOne("force");
                final boolean force = optForce.isPresent() ? optForce.get() : false;

                try {
                    Special.instance.getInstanceManager().endInstance(world.getName(), force);
                } catch (UnknownInstanceException e) {
                    throw new CommandException(Text.of("Unable to end instance [" + world.getName() + "], is it running?"), e);
                }

                return CommandResult.success();
            })
            .build();
    private static final CommandSpec joinCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.join")
            .description(Text.of("Joins an instance"))
            .extendedDescription(Text.of("Joins an instance")) // TODO More descriptive
            .arguments(world(Text.of("world")), optional(player(Text.of("player"))))
            .executor((src, args) -> {
                final Optional<WorldProperties> optProperties = args.getOne("world");
                final World world;
                if (optProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optProperties.get().getWorldName()).orElseThrow(() -> new
                            CommandException(Text.of("World provided is not online!")));
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

                Optional<Instance> instance = Special.instance.getInstanceManager().getInstance(world.getName());
                if(!instance.isPresent()) {
                    throw new CommandException(Text.of("Instance [" + world.getName() + "] is not a valid instance, is it running?"));
                }
                instance.get().spawnPlayer(player);

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
            .child(registerCommand, "register", "r")
            .child(createCommand, "create", "c")
            .child(startCommand, "start", "s")
            .child(endCommand, "end", "e")
            .child(joinCommand, "join", "j")
            .build();
}
