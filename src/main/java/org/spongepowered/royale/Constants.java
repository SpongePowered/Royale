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

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.DefaultedRegistryType;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.royale.instance.InstanceType;
import org.spongepowered.royale.instance.gen.InstanceMutator;
import org.spongepowered.royale.template.ComponentTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class Constants {

    private Constants() {
    }

    public static final class Plugin {
        public static final String ID = "royale";
        public static final DefaultedRegistryType<InstanceMutator> INSTANCE_MUTATOR = RegistryType.of(RegistryRoots.SPONGE, ResourceKey.of(Plugin.ID, "instance_mutator")).asDefaultedType(() -> Sponge.getServer().registries());
        public static final DefaultedRegistryType<InstanceType> INSTANCE_TYPE = RegistryType.of(RegistryRoots.SPONGE, ResourceKey.of(Plugin.ID, "instance_type")).asDefaultedType(() -> Sponge.getServer().registries());
    }

    public static final class Map {

        public static final Path INSTANCE_TYPES_FOLDER = Royale.instance.getConfigFile().resolve("types");

        public static final List<ResourceKey> DEFAULT_MAP_MUTATOR_IDS = new ArrayList<>();

        public static final Set<InstanceMutator> DEFAULT_MAP_MUTATORS = new HashSet<>();

        public static final int DEFAULT_CENTER_X = 0;
        public static final int DEFAULT_CENTER_Z = 0;
        public static final int DEFAULT_MAP_LENGTH = 250;
        public static final int DEFAULT_MAP_WIDTH = 250;
        public static final int DEFAULT_MAP_MIN_Y = 0;
        public static final int DEFAULT_MAP_MAX_Y = 256;
        public static final ComponentTemplate DEFAULT_TEXT_TEMPLATE_NAME = new ComponentTemplate("<red><pl_sponge:name></red>");
        public static final int DEFAULT_WORLD_BORDER_CENTER_X = 0;
        public static final int DEFAULT_WORLD_BORDER_CENTER_Z = 0;
        public static final int DEFAULT_WORLD_BORDER_RADIUS = 250;

        static {
            if (Files.notExists(INSTANCE_TYPES_FOLDER)) {
                try {
                    Files.createDirectories(INSTANCE_TYPES_FOLDER);
                } catch (final IOException e) {
                    throw new RuntimeException(String.format("Failed to create maps directory '%s'!", INSTANCE_TYPES_FOLDER));
                }
            }

            DEFAULT_MAP_MUTATOR_IDS.add(ResourceKey.of(Royale.instance.getPlugin().getMetadata().getId(), "player_spawn"));

            DEFAULT_MAP_MUTATORS.add(Sponge.getServer().registries().registry(Plugin.INSTANCE_MUTATOR).findValue(ResourceKey.of(Plugin.ID, "player_spawn")).get());
        }

        private Map() {
        }

        public static final class Round {

            public static final List<ItemStackSnapshot> DEFAULT_ITEMS = new LinkedList<>();


            public static final int DEFAULT_START_LENGTH = 5;
            public static final int DEFAULT_LENGTH = 300;
            public static final int DEFAULT_END_LENGTH = 10;
            public static final int DEFAULT_AUTOMATIC_START_PLAYER_COUNT = 6;
            public static final ComponentTemplate DEFAULT_TEXT_TEMPLATE_START = new ComponentTemplate("<green>Battle!</green>");
            public static final ComponentTemplate DEFAULT_TEXT_TEMPLATE_END =
                    new ComponentTemplate("<pl_sponge:name> <yellow>is the winner!</yellow>");

            static {
                DEFAULT_ITEMS.add(ItemStack.of(ItemTypes.STONE_SWORD, 1).createSnapshot());
                DEFAULT_ITEMS.add(ItemStack.of(ItemTypes.BOW, 1).createSnapshot());
                DEFAULT_ITEMS.add(ItemStack.of(ItemTypes.STONE_AXE, 1).createSnapshot());
                DEFAULT_ITEMS.add(ItemStack.of(ItemTypes.STONE_PICKAXE, 1).createSnapshot());
                DEFAULT_ITEMS.add(ItemStack.of(ItemTypes.ARROW, 5).createSnapshot());
            }

            private Round() {
            }
        }

        public static final class Lobby {

            public static final ResourceKey LOBBY_WORLD_KEY = ResourceKey.of(Plugin.ID, "lobby");
            public static final String SIGN_HEADER = "Join Game";

            static final WorldTemplate LOBBY_TEMPLATE = WorldTemplate.builder().from(WorldTemplate.overworld())
                    .key(Lobby.LOBBY_WORLD_KEY)
                    .loadOnStartup(true)
                    .difficulty(Difficulties.EASY)
                    .performsSpawnLogic(true)
                    .pvp(false)
                    .serializationBehavior(SerializationBehavior.AUTOMATIC_METADATA_ONLY)
                    .build();

            private Lobby() {
            }
        }
    }

    public static final class Permissions {

        public static final String ADMIN = Plugin.ID + ".admin";

        private Permissions() {
        }
    }
}
