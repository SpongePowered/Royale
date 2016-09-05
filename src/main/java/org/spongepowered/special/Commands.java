package org.spongepowered.special;

import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.string;

import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.special.configuration.MappedConfigurationAdapter;
import org.spongepowered.special.map.MapConfiguration;
import org.spongepowered.special.map.MapRegistryModule;
import org.spongepowered.special.map.MapType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class Commands {

    private static final CommandSpec registerCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.register")
            .description(Text.of())
            .extendedDescription(Text.of())
            .arguments(string(Text.of("id")), string(Text.of("name")), optional(string(Text.of("template"))))
            .executor((src, args) -> {
                final String id = args.<String>getOne("id").orElse(null);
                if (Sponge.getRegistry().getType(MapType.class, id).isPresent()) {
                    throw new CommandException(Text.of("Map has already been registered!"));
                }

                final String name = args.<String>getOne("name").orElse(null);

                final String template = args.<String>getOne("template").orElse(id);
                final Path templatePath = Constants.Map.PATH_CONFIG_TEMPLATES.resolve(template);

                if (Files.notExists(templatePath)) {
                    throw new CommandException(Text.of("Failed to register map type [", id, "] as template [", template, "] does not exist in [",
                            Constants.Map.PATH_CONFIG_TEMPLATES, "]!"));
                }

                if (Files.notExists(templatePath.resolve("level.dat"))) {
                    throw new CommandException(Text.of("Failed to register map type [", id, "] as template [", template, "] in [",
                            Constants.Map.PATH_CONFIG_TEMPLATES, "] is not a valid world!"));
                }

                final Path configPath = Constants.Map.PATH_CONFIG_MAPS.resolve(id + ".conf");
                final MappedConfigurationAdapter<MapConfiguration> adapter = new MappedConfigurationAdapter<>(MapConfiguration
                        .class, ConfigurationOptions.defaults(), configPath);

                try {
                    adapter.load();
                    adapter.getConfig().general.name = name;
                    adapter.getConfig().general.template = template;
                    adapter.save();
                } catch (ObjectMappingException | IOException e) {
                    throw new CommandException(Text.of("Failed to register map type [", id, "]!"), e);
                }


                MapRegistryModule.getInstance().registerAdditionalCatalog(MapType.builder().from(adapter.getConfig()).build(id));
                return CommandResult.success();
            })
            .build();

    static final CommandSpec startCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.start")
            .description(Text.of("Starts a game"))
            .extendedDescription(Text.of("Starts a game")) // TODO More descriptive
            .executor((src, args) -> {
                final Optional<WorldProperties> optProperties = args.getOne("world");
                final World world;
                if (optProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optProperties.get().getWorldName()).orElseThrow(() -> new
                            CommandException(Text.of("World provided is not online!")));
                } else if (src instanceof Player){
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }

                // TODO: Start game

                return CommandResult.success();
            })
            .build();

    static final CommandSpec endCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.end")
            .description(Text.of("Ends a game"))
            .extendedDescription(Text.of("Ends a game")) // TODO More descriptive
            .executor((src, args) -> {
                final Optional<WorldProperties> optProperties = args.getOne("world");
                final World world;
                if (optProperties.isPresent()) {
                    world = Sponge.getServer().getWorld(optProperties.get().getWorldName()).orElseThrow(() -> new
                            CommandException(Text.of("World provided is not online!")));
                } else if (src instanceof Player){
                    world = ((Player) src).getWorld();
                } else {
                    throw new CommandException(Text.of("World was not provided!"));
                }

                // TODO: End game

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
            .child(startCommand, "start", "s")
            .child(endCommand, "end", "e")
            .build();
}
