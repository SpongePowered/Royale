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
package org.spongepowered.special.instance.configuration.category;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.special.Constants;
import org.spongepowered.special.configuration.AbstractConfigurationCategory;

import java.util.List;

@ConfigSerializable
public final class GeneralCategory extends AbstractConfigurationCategory {

    @Setting(comment = "Name of this type. Shown on the in-game scoreboard, etc.")
    public String name = null;

    @Setting(value = "name-template", comment = "Template used to display the name to the user.")
    public TextTemplate nameTemplate = Constants.Map.DEFAULT_TEXT_TEMPLATE_NAME;

    @Setting(value = "center-x", comment = "The center of the map on the x-axis. 'min-x' and 'max-x' are relative to this coordinate.")
    public int centerX = Constants.Map.DEFAULT_CENTER_X;

    @Setting(value = "center-z", comment = "The center of the map on the z-axis. 'min-z' and 'max-z' are relative to this coordinate.")
    public int centerZ = Constants.Map.DEFAULT_CENTER_Z;

    @Setting(value = "min-x",
            comment = "The minimum x-coordinate of the map, relative to center-x. This is inclusive, so 'center-x + min-x' will be the actual "
                    + "minimum coordinate.")
    public int minX = -Constants.Map.DEFAULT_MAP_WIDTH;

    @Setting(value = "max-x",
            comment = "The maximum x-coordinate of the map, relative to center-x. This is exclusive, so 'center-x + max-x' will be one greater than"
                    + " the actual maximum coordinate.")
    public int maxX = Constants.Map.DEFAULT_MAP_WIDTH;

    @Setting(value = "min-y", comment = "The minimum y-coordinate of the map.")
    public int minY = Constants.Map.DEFAULT_MAP_MIN_Y;

    @Setting(value = "max-y", comment = "The maximum y-coordinate of the map.")
    public int maxY = Constants.Map.DEFAULT_MAP_MAX_Y;

    @Setting(value = "min-z",
            comment = "The minimum z-coordinate of the map, relative to center-z. This is inclusive, so 'center-z + min-z' will be the actual "
                    + "minimum coordinate")
    public int minZ = -Constants.Map.DEFAULT_MAP_LENGTH;

    @Setting(value = "max-z",
            comment = "The maximum z-coordinate of the map, relative to center-z. This is exclusive, so 'center-z + max-z' will be one greater than"
                    + " the actual maximum coordinate.")
    public int maxZ = Constants.Map.DEFAULT_MAP_LENGTH;

    @Setting(value = "world-border-center-x", comment = "The center of the world border on the x-axis")
    public int worldBorderCenterX = Constants.Map.DEFAULT_WORLD_BORDER_CENTER_X;

    @Setting(value = "world-border-center-z", comment = "The center of the world border on the z-axis")
    public int worldBorderCenterZ = Constants.Map.DEFAULT_WORLD_BORDER_CENTER_Z;

    @Setting(value = "world-border-radius", comment = "The radius of the world border")
    public int worldBorderRadius = Constants.Map.DEFAULT_WORLD_BORDER_RADIUS;

    @Setting(value = "map-mutators", comment = "Map Mutators to apply after the instance is loaded.")
    public List<String> mapMutators = Constants.Map.DEFAULT_MAP_MUTATOR_IDS;

}
