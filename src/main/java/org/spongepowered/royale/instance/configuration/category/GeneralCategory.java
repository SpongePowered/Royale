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
package org.spongepowered.royale.instance.configuration.category;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.configuration.AbstractConfigurationCategory;
import org.spongepowered.royale.template.ComponentTemplate;

import java.util.List;

@ConfigSerializable
public final class GeneralCategory extends AbstractConfigurationCategory {

    @Setting
    @Comment("Name of this type. Shown on the in-game scoreboard, etc.")
    public String name;

    @Setting
    @Comment("Template used to display the name to the user.")
    public ComponentTemplate nameTemplate = Constants.Map.DEFAULT_TEXT_TEMPLATE_NAME;

    @Setting
    @Comment("The center of the map on the x-axis. 'min-x' and 'max-x' are relative to this coordinate.")
    public int centerX = Constants.Map.DEFAULT_CENTER_X;

    @Setting
    @Comment("The center of the map on the z-axis. 'min-z' and 'max-z' are relative to this coordinate.")
    public int centerZ = Constants.Map.DEFAULT_CENTER_Z;

    @Setting
    @Comment("The minimum x-coordinate of the map, relative to center-x. This is inclusive, so 'center-x + min-x' will be the actual "
                     + "minimum coordinate.")
    public int minX = -Constants.Map.DEFAULT_MAP_WIDTH;

    @Setting
    @Comment("The maximum x-coordinate of the map, relative to center-x. This is exclusive, so 'center-x + max-x' will be one greater than"
                     + " the actual maximum coordinate.")
    public int maxX = Constants.Map.DEFAULT_MAP_WIDTH;

    @Setting
    @Comment("The minimum y-coordinate of the map.")
    public int minY = Constants.Map.DEFAULT_MAP_MIN_Y;

    @Setting
    @Comment("The maximum y-coordinate of the map.")
    public int maxY = Constants.Map.DEFAULT_MAP_MAX_Y;

    @Setting
    @Comment("The minimum z-coordinate of the map, relative to center-z. This is inclusive, so 'center-z + min-z' will be the actual "
                     + "minimum coordinate")
    public int minZ = -Constants.Map.DEFAULT_MAP_LENGTH;

    @Setting
    @Comment("The maximum z-coordinate of the map, relative to center-z. This is exclusive, so 'center-z + max-z' will be one greater than"
                     + " the actual maximum coordinate.")
    public int maxZ = Constants.Map.DEFAULT_MAP_LENGTH;

    @Setting
    @Comment("The center of the world border on the x-axis")
    public int worldBorderCenterX = Constants.Map.DEFAULT_WORLD_BORDER_CENTER_X;

    @Setting
    @Comment("The center of the world border on the z-axis")
    public int worldBorderCenterZ = Constants.Map.DEFAULT_WORLD_BORDER_CENTER_Z;

    @Setting
    @Comment("The radius of the world border")
    public int worldBorderRadius = Constants.Map.DEFAULT_WORLD_BORDER_RADIUS;

    @Setting
    @Comment("Map Mutators to apply after the instance is loaded.")
    public List<ResourceKey> mapMutators = Constants.Map.DEFAULT_MAP_MUTATOR_IDS;
}
