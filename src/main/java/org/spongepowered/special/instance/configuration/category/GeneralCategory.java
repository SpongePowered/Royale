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

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public final class GeneralCategory extends AbstractConfigurationCategory {

    @Setting(comment = "Name of this instance. Used to display to clients.")
    public String name = null;

    @Setting(value = "name-template", comment = "Template used to display the name to the user.")
    public TextTemplate nameTemplate = Constants.Map.DEFAULT_TEXT_TEMPLATE_NAME;

    @Setting(comment = "Name of the template to use (within the saves directory of the server).")
    public String template = null;

    @Setting(value = "map-length", comment = "The map's length. Used to handle unknown spawns, end of round player cleanup, and other things. "
            + "Specifying -1 tells the plugin to treat this value as unknown. Default (" + Constants.Map.DEFAULT_MAP_LENGTH + ").")
    public int mapLength = Constants.Map.DEFAULT_MAP_LENGTH;

    @Setting(value = "map-width", comment = "The map's width. Used to handle unknown spawns, end of round player cleanup, and other things. "
            + "Specifying -1 tells the plugin to treat this value as unknown. Default (" + Constants.Map.DEFAULT_MAP_WIDTH + ").")
    public int mapWidth = Constants.Map.DEFAULT_MAP_WIDTH;

    @Setting(value = "map-mutators", comment = "Map Mutators to apply after the template is loaded.")
    public List<String> mapMutators = new ArrayList<>();

}
