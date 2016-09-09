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
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.special.Constants;
import org.spongepowered.special.configuration.AbstractConfigurationCategory;

import java.util.List;

@ConfigSerializable
public final class RoundCategory extends AbstractConfigurationCategory {

    @Setting(value = "default-items", comment = "Default items to give players. Order of items will be inserted into the hotbar, left to right.")
    public final List<ItemStackSnapshot> defaultItems = Constants.Map.Round.DEFAULT_ITEMS;

    @Setting(comment = "Countdown until round starts (in seconds). Specifying -1 means the round will start the moment the instance starts. Default"
            + " (" + Constants.Map.Round.DEFAULT_START_LENGTH + ").")
    public long start = Constants.Map.Round.DEFAULT_START_LENGTH;

    @Setting(value = "start-template", comment = "Template used to display the round start.")
    public TextTemplate startTemplate = Constants.Map.Round.DEFAULT_TEXT_TEMPLATE_START;

    @Setting(comment = "Length of the round (in seconds). Specifying -1 means the instance goes until a winner is found. Default (" + Constants.Map
            .Round.DEFAULT_LENGTH + ").")
    public long length = Constants.Map.Round.DEFAULT_LENGTH;

    @Setting(comment = "Length of the time after round ends until everyone is kicked (in seconds). Specifying -1 means the instance must be "
            + "manually terminated. Default (" + Constants.Map.Round.DEFAULT_END_LENGTH + ").")
    public long end = Constants.Map.Round.DEFAULT_END_LENGTH;

    @Setting(value = "end-template", comment = "Template used to display the winner.")
    public TextTemplate endTemplate = Constants.Map.Round.DEFAULT_TEXT_TEMPLATE_END;

    @Setting(value = "players-to-start-round-automatically", comment = "If specified, once the instance has this amount of players, it will "
            + "automatically start. Specifying -1 means the instance must be started manually. Default (" + Constants.Map.Round
            .DEFAULT_AUTOMATIC_START_PLAYER_COUNT + ").")
    public int automaticStartPlayerCount = Constants.Map.Round.DEFAULT_AUTOMATIC_START_PLAYER_COUNT;
}
