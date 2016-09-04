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

import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.GeneratorTypes;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.WorldArchetypes;
import org.spongepowered.api.world.difficulty.Difficulties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Constants {

    static final class Meta {

        // TODO Gradle Replacements

        static final String ID = "special";
        static final String NAME = "Special";
        static final String VERSION = "1.10.2-r5.0";
        static final String AUTHORS = "SpongePowered";
        static final String URL = "https://www.spongepowered.org";
        static final String DESCRIPTION = "Skywars but with a Sponge twist. Anaheim, CA - Minecon 2016";

        private Meta() {}
    }

    public static final class Map {

        static final Path PATH_CONFIG_MAPS = Special.instance.configPath.resolve("maps");

        public static final TextTemplate TEMPLATE_NAME_TITLE = TextTemplate.of(TextTemplate.arg("name").color(TextColors.RED));
        public static final TextTemplate TEMPLATE_ROUND_START_TITLE = TextTemplate.of(TextColors.GREEN, "Battle!");
        public static final TextTemplate TEMPLATE_ANNOUNCE_WINNER_TITLE = TextTemplate.of(TextTemplate.arg("winner"), TextColors.YELLOW, " is the "
                + "winner!");

        public static final List<ItemStackSnapshot> defaultKitItems = new ArrayList<>();

        static {
            if (Files.notExists(PATH_CONFIG_MAPS)) {
                try {
                    Files.createDirectories(PATH_CONFIG_MAPS);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create maps configuration directory [" + PATH_CONFIG_MAPS + "]!");
                }
            }

            defaultKitItems.add(ItemStack.of(ItemTypes.STONE_SWORD, 1).createSnapshot());
            defaultKitItems.add(ItemStack.of(ItemTypes.BOW, 1).createSnapshot());
            defaultKitItems.add(ItemStack.of(ItemTypes.STONE_AXE, 1).createSnapshot());
            defaultKitItems.add(ItemStack.of(ItemTypes.STONE_PICKAXE, 1).createSnapshot());
            defaultKitItems.add(ItemStack.of(ItemTypes.ARROW, 5).createSnapshot());
        }

        private Map() {}

        static final class Lobby {

            static final WorldArchetype lobbyArchetype = WorldArchetype.builder().from(WorldArchetypes.THE_VOID)
                    .gameMode(GameModes.ADVENTURE)
                    .loadsOnStartup(true)
                    .difficulty(Difficulties.EASY)
                    .generateSpawnOnLoad(true)
                    .generator(GeneratorTypes.THE_END) // TODO Remove when Inscrutable has Lobby map
                    .pvp(false)
                    .keepsSpawnLoaded(true)
                    .build(Meta.ID + ":lobby", "Lobby");

            private Lobby() {}
        }
    }

    private Constants() {}
}
