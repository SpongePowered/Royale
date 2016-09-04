package org.spongepowered.special;

import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.special.configuration.MappedConfigurationAdapter;
import org.spongepowered.special.map.MapRegistryModule;
import org.spongepowered.special.map.MapType;
import org.spongepowered.special.map.MapConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class Commands {

    private static final CommandSpec registerCommand = CommandSpec.builder()
            .permission(Constants.Meta.ID + ".command.register")
            .description(Text.of())
            .extendedDescription(Text.of())
            .arguments(GenericArguments.string(Text.of("map")), GenericArguments.string(Text.of("templatePath")))
            .executor((src, args) -> {
                // TODO Exception messages
                final String mapId = args.<String>getOne("map").orElseThrow(() -> new CommandException(Text.of("Id")));
                if (Sponge.getRegistry().getType(MapType.class, mapId).isPresent()) {
                    throw new CommandException(Text.of("Map has already been registered!"));
                }

                final String mapTemplatePath = args.<String>getOne("templatePath").orElseThrow(() -> new CommandException(Text.of
                        ("TemplatePath")));

                final Path templatePath = Sponge.getGame().getGameDirectory().resolve(mapTemplatePath);
                if (Files.notExists(templatePath)) {
                    throw new CommandException(Text.of("Template path ", templatePath, " does not exist!"));
                }

                // TODO Check if template folder has a level.dat

                final Path configPath = Constants.Map.PATH_CONFIG_MAPS.resolve(mapId + ".conf");
                final MappedConfigurationAdapter<MapConfiguration> adapter = new MappedConfigurationAdapter<>(MapConfiguration
                        .class, ConfigurationOptions.defaults(), configPath);
                try {
                    adapter.load();
                } catch (IOException | ObjectMappingException e) {
                    throw new CommandException(Text.of("Failed to load map configuration!"));
                }

                adapter.getConfig().general.templatePath = mapTemplatePath;

                try {
                    adapter.save();
                } catch (IOException | ObjectMappingException e) {
                    throw new CommandException(Text.of("Failed to save template path information!"));
                }

                final MapType mapType = new MapType(mapId, adapter);
                MapRegistryModule.getInstance().registerAdditionalCatalog(mapType);

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
            .build();
}
